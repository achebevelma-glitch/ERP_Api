package com.example.ERP_Api.Contoller;

import com.example.ERP_Api.Service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin only. SecurityConfig already ends with anyRequest().hasRole("ADMIN"),
 * so these URLs need no extra rule.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public NotificationService.Overview overview() {
        return notifications.overview();
    }

    @PatchMapping("/{id}/read")
    public Map<String, Boolean> read(@PathVariable Long id) {
        notifications.markRead(id);
        return Map.of("ok", true);
    }

    @PatchMapping("/read-all")
    public Map<String, Integer> readAll() {
        return Map.of("updated", notifications.markAllRead());
    }
}