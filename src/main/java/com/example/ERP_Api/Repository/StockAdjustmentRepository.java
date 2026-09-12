package com.example.ERP_Api.Repository;

import com.example.ERP_Api.Entity.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {
    List<StockAdjustment> findByInventoryIdOrderByCreatedAtDesc(Long inventoryId);
}