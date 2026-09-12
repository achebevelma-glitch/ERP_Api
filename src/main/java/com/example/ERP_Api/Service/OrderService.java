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

    public OrderService(OrderRepository orderRepository,
                        InventoryRepository inventoryRepository,
                        ProductRepository productRepository,
                        CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
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

        Inventory inventory = inventoryRepository.findByProductIdForUpdate(req.productId())
                .orElseThrow(() -> new IllegalArgumentException("No inventory record for product: " + req.productId()));

        if (inventory.getQuantity() < req.quantity()) {
            throw new IllegalStateException("Insufficient stock for product: " + req.productId());
        }

        inventory.setQuantity(inventory.getQuantity() - req.quantity());
        inventoryRepository.save(inventory);

        long amountCents = product.getPriceCents() * req.quantity();

        Order order = new Order(customer, product, req.quantity(), amountCents,
                PaymentStatus.DUE, ShippingStatus.PENDING);

        return orderRepository.save(order);
    }


    public Order transitionStatus(Long id, ShippingStatus newStatus) {
        Order order = getById(id);
        if (!order.getShippingStatus().canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition order " + id + " from " + order.getShippingStatus() + " to " + newStatus);
        }
        order.setShippingStatus(newStatus);
        return orderRepository.save(order);
    }


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