package com.example.ERP_Api.Service;

import com.example.ERP_Api.Entity.*;
import com.example.ERP_Api.Repository.CustomerRepository;
import com.example.ERP_Api.Repository.InventoryRepository;
import com.example.ERP_Api.Repository.OrderRepository;
import com.example.ERP_Api.Repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;

    public OrderService(OrderRepository orderRepository,
                        InventoryRepository inventoryRepository,
                        ProductRepository productRepository,
                        CustomerRepository customerRepository,
                        InventoryService inventoryService,
                        NotificationService notificationService
    ) {
        this.orderRepository = orderRepository;
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
    }

    public List<Order> list(ShippingStatus status) {
        if (status == null) return orderRepository.findAll();
        return orderRepository.findByShippingStatus(status);
    }

    public Order getById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    public record CreateOrderRequest(Long customerId, Long productId, Integer quantity) {}


    @Transactional
    public Order create(CreateOrderRequest req) {
        Customer customer = customerRepository.findById(req.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + req.customerId()));
        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + req.productId()));

        // Row lock so two simultaneous orders cannot both take the last unit.
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(req.productId())
                .orElseThrow(() -> new IllegalArgumentException("No inventory record for product: " + req.productId()));

        if (inventory.getQuantity() < req.quantity()) {
            throw new IllegalStateException("Insufficient stock for product: " + req.productId());
        }

        long amountCents = product.getPriceCents() * req.quantity();

        Order order = new Order(customer, product, req.quantity(), amountCents,
                PaymentStatus.DUE, ShippingStatus.PENDING);
        order = orderRepository.save(order); // saved first so the history row can name the order

        //  reduce stock through InventoryService so a stock adjustment row
        // is written. Same transaction: the order, the new quantity and the
        // history row are saved together or not at all.
        inventoryService.adjustStock(req.productId(), -req.quantity(), "Order #" + order.getId());

        // NEW: tell the admins. Same transaction, so the notification exists only
        // if the order was really saved.
        notificationService.orderCreated(order);

        return order;
    }


    // FIX 1: @Transactional here (and on cancel) so the status change and the
    // stock return succeed or fail together. cancel() calls transitionStatus()
    // from inside the class, which skips Spring's proxy, so both need it.
    @Transactional
    public Order transitionStatus(Long id, ShippingStatus newStatus) {
        Order order = getById(id);
        if (!order.getShippingStatus().canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition order " + id + " from " + order.getShippingStatus() + " to " + newStatus);
        }

        if (newStatus == ShippingStatus.DECLINED) {
            // A declined order never left the warehouse: put its quantity back.
            // Only a PENDING order can be declined (see ShippingStatus), so this
            // can run at most once per order.
            inventoryService.adjustStock(order.getProduct().getId(), order.getQuantity(),
                    "Order #" + id + " declined");
        }

        order.setShippingStatus(newStatus);
        return orderRepository.save(order);
    }


    @Transactional
    public Order cancel(Long id) {
        return transitionStatus(id, ShippingStatus.DECLINED);
    }

    public Map<String, Long> getStats() {
        return Map.of(
                "total", orderRepository.count(),
                "pending", (long) orderRepository.findByShippingStatus(ShippingStatus.PENDING).size(),
                "declined", (long) orderRepository.findByShippingStatus(ShippingStatus.DECLINED).size()
        );
    }
}