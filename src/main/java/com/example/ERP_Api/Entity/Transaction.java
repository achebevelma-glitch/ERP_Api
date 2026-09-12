package com.example.ERP_Api.Entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date; // the accounting date (e.g. "2026-09-01"), separate from createdAt below

    private String description;
    private String category; // e.g. "Sales", "Payroll", "Facilities", "Inventory"

    // Cents, not double — consistent with Order.amountCents and
    // Product.priceCents. Aggregating doubles in a SUM query for the
    // revenue/expense summary cards is where float drift actually bites.
    private Long amountCents;

    @Enumerated(EnumType.STRING)
    private TransactionType type; // INCOME, EXPENSE

    // Optional link back to the order that generated this transaction —
    // not every transaction has one (rent, payroll don't), so this stays
    // nullable. Lets you trace "this $950 income row" back to Order #1042.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private Instant createdAt;

    public Transaction() {
    }

    public Transaction(LocalDate date, String description, String category,
                       Long amountCents, TransactionType type) {
        this.date = date;
        this.description = description;
        this.category = category;
        this.amountCents = amountCents;
        this.type = type;
        this.createdAt = Instant.now();
    }

    // ===== Getters and setters =====

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(Long amountCents) {
        this.amountCents = amountCents;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}