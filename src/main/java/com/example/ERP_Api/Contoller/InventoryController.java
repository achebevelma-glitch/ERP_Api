package com.example.ERP_Api.Contoller;

import com.example.ERP_Api.Entity.Inventory;
import com.example.ERP_Api.Entity.StockStatus;
import com.example.ERP_Api.Service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<Inventory> list(@RequestParam(required = false) StockStatus status) {
        return inventoryService.list(status);
    }

    @GetMapping("/{productId}")
    public Inventory get(@PathVariable Long productId) {
        return inventoryService.getByProductId(productId);
    }


    @PostMapping("/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Inventory initialize(@PathVariable Long productId, @RequestBody InitInventoryRequest req) {
        return inventoryService.initialize(productId, req.quantity());
    }

    public record InitInventoryRequest(int quantity) {}

    @PostMapping("/{productId}/adjustments")
    public Inventory adjust(@PathVariable Long productId, @RequestBody StockAdjustmentRequest req) {
        return inventoryService.adjustStock(productId, req.delta(), req.reason());
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return inventoryService.getStats();
    }

    public record StockAdjustmentRequest(int delta, String reason) {}
}