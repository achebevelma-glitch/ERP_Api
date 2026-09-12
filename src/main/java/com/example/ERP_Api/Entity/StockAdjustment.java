package com.example.ERP_Api.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    @JsonIgnoreProperties("adjustments")
    private Inventory inventory;


    @Setter
    private Integer delta;//negative for sale , positive for restock

    @Setter
    private String reason; //  "sale", "restock", "damaged", "correction"

    private Instant createdAt;

    public StockAdjustment() {
    }

    public StockAdjustment(Inventory inventory, Integer delta, String reason) {
        this.inventory = inventory;
        this.delta = delta;
        this.reason = reason;
        this.createdAt = Instant.now();
    }


}
