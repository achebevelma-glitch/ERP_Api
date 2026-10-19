package com.example.ERP_Api.Service;


import com.example.ERP_Api.Entity.*;
import com.example.ERP_Api.Repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
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

    public InvoiceService(InvoiceRepository invoiceRepository,
                          CustomerRepository customerRepository,
                          ProductRepository productRepository,
                          InventoryRepository inventoryRepository,
                          OrderRepository orderRepository) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
    }

    // ===== Request / response shapes =====

    public record CheckoutLine(Long productId, Integer quantity) {}

    public record CheckoutRequest(String name, String email, String phone, String address,
                                  String paymentMethod, List<CheckoutLine> items) {}

    public record InvoiceItemDto(Long productId, String name, String sku, Integer quantity,
                                 Long unitPriceCents, Long lineTotalCents) {}

    public record InvoiceDto(Long id, String invoiceNumber, Instant createdAt,
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

        // Merge duplicate lines. TreeMap sorts by product id so stock rows are always
        // locked in the same order, which avoids deadlocks between concurrent checkouts.
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

            // The price always comes from the database, never from the browser
            long unit = product.getPriceCents() == null ? 0L : product.getPriceCents();
            long lineTotal = unit * qty;
            total += lineTotal;

            // Also record it as a normal order so it shows on the Orders page and dashboard
            orderRepository.save(new Order(customer, product, qty, lineTotal,
                    PaymentStatus.DUE, ShippingStatus.PENDING));

            InvoiceItem item = new InvoiceItem();
            item.setProductId(productId);
            item.setProductName(product.getName());
            item.setSku(product.getSku());
            item.setQuantity(qty);
            item.setUnitPriceCents(unit);
            invoice.addItem(item);
        }

        invoice.setTotalCents(total);
        invoice = invoiceRepository.save(invoice);
        invoice.setInvoiceNumber(String.format("INV-%d-%05d", Year.now().getValue(), invoice.getId()));
        invoice = invoiceRepository.save(invoice);

        return toDto(invoice);
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
        // Someone else's invoice looks exactly like one that doesn't exist
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
        return new InvoiceDto(i.getId(), i.getInvoiceNumber(), i.getCreatedAt(),
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
