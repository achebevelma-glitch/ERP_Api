package com.example.ERP_Api.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Setter
    private String name;
    @Setter
    @Column(unique = true, nullable = false)
    private String sku;
    @Setter
    private String category;
    @Setter
    private Long priceCents;
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
    private Instant createdAt;

    public Product() {
    }

    public Product(String name, String sku, String category, Long priceCents) {
        this.name = name;
        this.sku = sku;
        this.category = category;
        this.priceCents = priceCents;
        this.createdAt = Instant.now();
    }

}
