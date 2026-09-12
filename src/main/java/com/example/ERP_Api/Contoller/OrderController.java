package com.example.ERP_Api.Contoller;


import com.example.ERP_Api.Entity.Order;
import com.example.ERP_Api.Entity.ShippingStatus;
import com.example.ERP_Api.Service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<Order> list(@RequestParam(required = false) ShippingStatus status) {
        return orderService.list(status);
    }

    @GetMapping("/{id}")
    public Order get(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order create(@RequestBody OrderService.CreateOrderRequest req) {
        return orderService.create(req);
    }

    // Body: {"newStatus": "DELIVERED"}
    @PatchMapping("/{id}/status")
    public Order updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest req) {
        return orderService.transitionStatus(id, req.newStatus());
    }

    @DeleteMapping("/{id}")
    public Order cancel(@PathVariable Long id) {
        return orderService.cancel(id);
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return orderService.getStats();
    }

    public record UpdateStatusRequest(ShippingStatus newStatus) {}
}