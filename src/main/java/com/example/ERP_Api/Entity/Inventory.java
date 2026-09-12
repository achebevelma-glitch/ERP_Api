package com.example.ERP_Api.Entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    private Integer quantity;
    private Instant updatedAt;

    @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL)
    private List<StockAdjustment> adjustments = new ArrayList<>();

    public Inventory() {
    }

    public Inventory(Product product, Integer quantity) {
        this.product = product;
        this.quantity = quantity;
        this.updatedAt = Instant.now();
    }

    public StockStatus getStockStatus() {
        if (quantity == null || quantity == 0) return StockStatus.OUT_OF_STOCK;
        if (quantity < 10) return StockStatus.LOW_STOCK;
        return StockStatus.IN_STOCK;
    }

    // ===== Getters and setters =====

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        this.updatedAt = Instant.now();
    }

}