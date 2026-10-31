package com.example.ERP_Api.Service;

import com.example.ERP_Api.Entity.*;
import com.example.ERP_Api.Repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
public class InvoiceService {

    private static final Set<String> PAYMENT_METHODS = Set.of("card", "mobile", "bank", "cash");

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          CustomerRepository customerRepository,
                          ProductRepository productRepository,
                          InventoryRepository inventoryRepository,
                          OrderRepository orderRepository,
                          TransactionRepository transactionRepository,
                          InventoryService inventoryService,
                          NotificationService notificationService) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
    }

    // ===== Request / response shapes =====

    public record CheckoutLine(Long productId, Integer quantity) {}

    public record CheckoutRequest(String name, String email, String phone, String address,
                                  String paymentMethod, List<CheckoutLine> items) {}

    public record InvoiceItemDto(Long productId, String name, String sku, Integer quantity,
                                 Long unitPriceCents, Long lineTotalCents) {}

    public record InvoiceDto(Long id, String invoiceNumber, Instant createdAt, Instant paidAt,
                             String customerName, String customerEmail, String customerPhone,
                             String deliveryAddress, String paymentMethod, String status,
                             Long totalCents, List<InvoiceItemDto> items) {}

    // ===== Checkout =====

    @Transactional
    public InvoiceDto checkout(String accountEmail, CheckoutRequest req) {
        if (req == null || req.items() == null || req.items().isEmpty()) {
            throw bad("Your cart is empty.");
        }

        String name = limit(req.name(), 255, "Name");
        if (name.isEmpty()) throw bad("Please enter your name.");
        String email = limit(req.email(), 255, "Email");
        String phone = limit(req.phone(), 50, "Phone");
        String address = limit(req.address(), 500, "Address");

        String method = req.paymentMethod() == null ? "" : req.paymentMethod().trim().toLowerCase();
        if (!PAYMENT_METHODS.contains(method)) throw bad("Choose a valid payment method.");

        Map<Long, Integer> wanted = new TreeMap<>();
        for (CheckoutLine line : req.items()) {
            if (line == null || line.productId() == null || line.quantity() == null
                    || line.quantity() < 1 || line.quantity() > 1000) {
                throw bad("Invalid item in cart.");
            }
            wanted.merge(line.productId(), line.quantity(), Integer::sum);
        }

        Customer customer = customerRepository.findFirstByEmailIgnoreCase(accountEmail)
                .orElseGet(() -> customerRepository.save(new Customer(name, accountEmail, CustomerStatus.ACTIVE)));

        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);
        invoice.setCustomerName(name);
        invoice.setCustomerEmail(email.isEmpty() ? accountEmail : email);
        invoice.setCustomerPhone(phone);
        invoice.setDeliveryAddress(address);
        invoice.setPaymentMethod(method);
        invoice.setCreatedBy(accountEmail);

        long total = 0;
        for (Map.Entry<Long, Integer> entry : wanted.entrySet()) {
            Long productId = entry.getKey();
            int qty = entry.getValue();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Product not found: " + productId));

            Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            product.getName() + " is not available right now."));

            if (inventory.getQuantity() == null || inventory.getQuantity() < qty) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Not enough stock for " + product.getName() + ".");
            }
            inventory.setQuantity(inventory.getQuantity() - qty);
            inventoryRepository.save(inventory);

            long unit = product.getPriceCents() == null ? 0L : product.getPriceCents();
            long lineTotal = unit * qty;
            total += lineTotal;

            Order savedOrder = orderRepository.save(new Order(customer, product, qty, lineTotal,
                    PaymentStatus.DUE, ShippingStatus.PENDING));

            InvoiceItem item = new InvoiceItem();
            item.setProductId(productId);
            item.setProductName(product.getName());
            item.setSku(product.getSku());
            item.setQuantity(qty);
            item.setUnitPriceCents(unit);
            item.setOrderId(savedOrder.getId());
            invoice.addItem(item);
        }

        invoice.setTotalCents(total);
        invoice = invoiceRepository.save(invoice);
        invoice.setInvoiceNumber(String.format("INV-%d-%05d", Year.now().getValue(), invoice.getId()));
        invoice = invoiceRepository.save(invoice);

        // NEW: tell the admins. Same transaction as the order, so the notification
        // exists only if the order was really saved.
        notificationService.orderPlaced(invoice,
                wanted.values().stream().mapToInt(Integer::intValue).sum());

        return toDto(invoice);
    }

    // ===== Payment =====

    /**
     * Marks an invoice as paid: the invoice and its orders become PAID, and one INCOME
     * transaction (category "Sales") is recorded per order. This is what feeds
     * Total Sales and Net Profit on the dashboard.
     */
    @Transactional
    public InvoiceDto markPaid(Long id) {
        Invoice invoice = invoiceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        if (invoice.getPaymentStatus() != PaymentStatus.DUE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invoice " + invoice.getInvoiceNumber() + " is already "
                            + invoice.getPaymentStatus().name().toLowerCase() + ".");
        }

        LocalDate today = LocalDate.now();
        long unlinkedTotal = 0;

        for (InvoiceItem item : invoice.getItems()) {
            long lineTotal = item.getUnitPriceCents() * item.getQuantity();

            if (item.getOrderId() == null) {
                // Invoice created before lines were linked to orders
                unlinkedTotal += lineTotal;
                continue;
            }

            Order order = orderRepository.findById(item.getOrderId()).orElse(null);
            if (order == null || order.getShippingStatus() == ShippingStatus.DECLINED) {
                continue; // cancelled or deleted order: no income
            }

            order.setPaymentStatus(PaymentStatus.PAID);
            orderRepository.save(order);

            Transaction income = new Transaction(today,
                    "Invoice " + invoice.getInvoiceNumber() + " - " + item.getProductName(),
                    "Sales", lineTotal, TransactionType.INCOME);
            income.setOrder(order);
            transactionRepository.save(income);
        }

        if (unlinkedTotal > 0) {
            transactionRepository.save(new Transaction(today,
                    "Invoice " + invoice.getInvoiceNumber(), "Sales", unlinkedTotal, TransactionType.INCOME));
        }

        invoice.setPaymentStatus(PaymentStatus.PAID);
        invoice.setPaidAt(Instant.now());
        return toDto(invoiceRepository.save(invoice));
    }


    /**
     * Refunds a PAID invoice: the invoice and its paid orders become REFUNDED, and one
     * EXPENSE transaction (category "Refunds") is recorded per paid line. When restock is
     * true the refunded quantities go back into inventory.
     */
    @Transactional
    public InvoiceDto refund(Long id, boolean restock) {
        Invoice invoice = invoiceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        if (invoice.getPaymentStatus() == PaymentStatus.DUE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invoice " + invoice.getInvoiceNumber() + " has not been paid, so there is nothing to refund.");
        }
        if (invoice.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invoice " + invoice.getInvoiceNumber() + " is already refunded.");
        }

        LocalDate today = LocalDate.now();
        long unlinkedTotal = 0;

        for (InvoiceItem item : invoice.getItems()) {
            long lineTotal = item.getUnitPriceCents() * item.getQuantity();

            if (item.getOrderId() == null) {
                // Invoice created before lines were linked to orders
                unlinkedTotal += lineTotal;
            } else {
                Order order = orderRepository.findById(item.getOrderId()).orElse(null);
                // Only lines that were actually paid were counted as income
                if (order == null || order.getPaymentStatus() != PaymentStatus.PAID) {
                    continue;
                }

                order.setPaymentStatus(PaymentStatus.REFUNDED);
                orderRepository.save(order);

                Transaction expense = new Transaction(today,
                        "Refund " + invoice.getInvoiceNumber() + " - " + item.getProductName(),
                        "Refunds", lineTotal, TransactionType.EXPENSE);
                expense.setOrder(order);
                transactionRepository.save(expense);
            }

            if (restock && inventoryRepository.findByProductId(item.getProductId()).isPresent()) {
                inventoryService.adjustStock(item.getProductId(), item.getQuantity(), "refund");
            }
        }

        if (unlinkedTotal > 0) {
            transactionRepository.save(new Transaction(today,
                    "Refund " + invoice.getInvoiceNumber(), "Refunds", unlinkedTotal, TransactionType.EXPENSE));
        }

        invoice.setPaymentStatus(PaymentStatus.REFUNDED);
        invoice.setRefundedAt(Instant.now());
        return toDto(invoiceRepository.save(invoice));
    }

    // ===== Reading =====

    @Transactional(readOnly = true)
    public List<InvoiceDto> listAll() {
        return invoiceRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto> listMine(String email) {
        return invoiceRepository.findByCreatedByIgnoreCaseOrderByCreatedAtDesc(email)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public InvoiceDto get(Long id, String email, boolean admin) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        if (!admin && !invoice.getCreatedBy().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found");
        }
        return toDto(invoice);
    }

    // ===== Helpers =====

    private InvoiceDto toDto(Invoice i) {
        List<InvoiceItemDto> items = i.getItems().stream()
                .map(it -> new InvoiceItemDto(it.getProductId(), it.getProductName(), it.getSku(),
                        it.getQuantity(), it.getUnitPriceCents(),
                        it.getUnitPriceCents() * it.getQuantity()))
                .toList();
        return new InvoiceDto(i.getId(), i.getInvoiceNumber(), i.getCreatedAt(), i.getPaidAt(),
                i.getCustomerName(), i.getCustomerEmail(), i.getCustomerPhone(),
                i.getDeliveryAddress(), i.getPaymentMethod(), i.getPaymentStatus().name(),
                i.getTotalCents(), items);
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static String limit(String value, int max, String field) {
        String v = value == null ? "" : value.trim();
        if (v.length() > max) throw bad(field + " is too long.");
        return v;
    }
}