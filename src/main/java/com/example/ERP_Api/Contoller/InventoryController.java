package com.example.ERP_Api.Controller;

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

    // Creates the initial inventory row for a product that doesn't have one
    // yet. Body: {"quantity": 50}. This is the endpoint that was missing —
    // adjustments below can only modify an existing row, not create one.
    @PostMapping("/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Inventory initialize(@PathVariable Long productId, @RequestBody InitInventoryRequest req) {
        return inventoryService.initialize(productId, req.quantity());
    }

    public record InitInventoryRequest(int quantity) {}

    // Not a PATCH — quantity only ever changes through an audited
    // adjustment. Body: {"delta": -5, "reason": "sale"}
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