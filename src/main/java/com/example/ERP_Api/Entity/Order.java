package com.example.ERP_Api.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Every order belongs to exactly one customer. LAZY avoids pulling the
    // full Customers row (and its own order list) every time you just need
    // to render an order row.
    // @JsonIgnoreProperties("orders") here is the belt-and-suspenders half
    // of the same fix as Customers.orders: it stops the same cycle if an
    // Order is ever serialized as the top-level object with its customer
    // still holding a live "orders" list.
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnoreProperties("orders")
    private Customer customer;

    // NOTE: this models one product per order, matching what we've built
    // so far (Laptop / Phone / Tablet etc., one row = one order). A real
    // multi-item cart needs a separate OrderItem entity with its own
    // @ManyToOne back to Order and @ManyToOne to Product, plus a quantity
    // and unit price per line — say the word if you want that instead.
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Setter
    private Integer quantity;

    // Store money as cents, not double — avoids float drift in SUM queries
    // on the accounting side.
    @Setter
    private Long amountCents;

    @Setter
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus; // DUE, PAID, REFUNDED

    @Setter
    @Enumerated(EnumType.STRING)
    private ShippingStatus shippingStatus; // PENDING, DELIVERED, DECLINED

    private Instant createdAt;

    public Order() {
    }

    public Order(Customer customer, Product product, Integer quantity,
                 Long amountCents, PaymentStatus paymentStatus, ShippingStatus shippingStatus) {
        this.customer = customer;
        this.product = product;
        this.quantity = quantity;
        this.amountCents = amountCents;
        this.paymentStatus = paymentStatus;
        this.shippingStatus = shippingStatus;
        this.createdAt = Instant.now();
    }

    // ===== Getters and setters =====

}