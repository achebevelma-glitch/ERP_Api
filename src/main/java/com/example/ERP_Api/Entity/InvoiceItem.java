package com.example.ERP_Api.Entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "invoice_items")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    // Snapshot: the invoice must not change if the product is renamed or repriced later
    private Long productId;
    private String productName;
    private String sku;
    private Integer quantity;
    private Long unitPriceCents;
}
