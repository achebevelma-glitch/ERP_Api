package com.example.ERP_Api.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Instant;

@Getter
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    private LocalDate date; // the accounting date (e.g. "2026-09-01"), separate from createdAt below

    @Setter
    private String description;
    @Setter
    private String category; // e.g. "Sales", "Payroll", "Facilities", "Inventory"


    @Setter
    private Long amountCents;

    @Setter
    @Enumerated(EnumType.STRING)
    private TransactionType type; // INCOME, EXPENSE


    @Setter
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



}