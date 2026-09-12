package com.example.ERP_Api.Service;

import com.example.ERP_Api.Entity.Inventory;
import com.example.ERP_Api.Entity.StockAdjustment;
import com.example.ERP_Api.Entity.StockStatus;
import com.example.ERP_Api.Entity.Product;
import com.example.ERP_Api.Repository.InventoryRepository;
import com.example.ERP_Api.Repository.ProductRepository;
import com.example.ERP_Api.Repository.StockAdjustmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final ProductRepository productRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            StockAdjustmentRepository stockAdjustmentRepository,
                            ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.stockAdjustmentRepository = stockAdjustmentRepository;
        this.productRepository = productRepository;
    }


    @Transactional
    public Inventory initialize(Long productId, int quantity) {
        if (inventoryRepository.findByProductId(productId).isPresent()) {
            throw new IllegalStateException("Inventory already exists for product: " + productId);
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        return inventoryRepository.save(new Inventory(product, quantity));
    }

    public List<Inventory> list(StockStatus status) {
        List<Inventory> all = inventoryRepository.findAll();
        if (status == null) return all;
        return all.stream().filter(i -> i.getStockStatus() == status).toList();
    }

    public Inventory getByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("No inventory record for product: " + productId));
    }


    @Transactional
    public Inventory adjustStock(Long productId, int delta, String reason) {
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new IllegalArgumentException("No inventory record for product: " + productId));

        int newQuantity = inventory.getQuantity() + delta;
        if (newQuantity < 0) {
            throw new IllegalStateException("Adjustment would drop stock below zero for product: " + productId);
        }

        inventory.setQuantity(newQuantity);
        inventoryRepository.save(inventory);
        stockAdjustmentRepository.save(new StockAdjustment(inventory, delta, reason));

        return inventory;
    }

    public Map<String, Long> getStats() {
        List<Inventory> all = inventoryRepository.findAll();
        return Map.of(
                "total", (long) all.size(),
                "lowStock", all.stream().filter(i -> i.getStockStatus() == StockStatus.LOW_STOCK).count(),
                "outOfStock", all.stream().filter(i -> i.getStockStatus() == StockStatus.OUT_OF_STOCK).count()
        );
    }
}