package com.example.ERP_Api.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Entity
@Table(name = "stock_adjustments")
public class StockAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    // Signed delta: negative for a sale, positive for a restock. This is
    // what /api/inventory/{productId}/adjustments writes to, instead of
    // letting a raw PATCH overwrite quantity directly.
    @Setter
    private Integer delta;

    @Setter
    private String reason; // e.g. "sale", "restock", "damaged", "correction"

    private Instant createdAt;

    public StockAdjustment() {
    }

    public StockAdjustment(Inventory inventory, Integer delta, String reason) {
        this.inventory = inventory;
        this.delta = delta;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    // ===== Getters and setters =====

}
