package com.IT_market.controller;

import com.IT_market.model.Notification;
import com.IT_market.model.User;
import com.IT_market.security.AuthInterceptor;
import com.IT_market.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    
    private final NotificationService notificationService;
    
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    @GetMapping
    public ResponseEntity<?> getUserNotifications(
            @RequestParam(defaultValue = "10") int limit) {
        
        User currentUser = AuthInterceptor.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(createErrorResponse("Unauthorized"));
        }
        
        List<Notification> notifications = notificationService.getUserNotifications(currentUser.getId(), limit);
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount() {
        User currentUser = AuthInterceptor.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(createErrorResponse("Unauthorized"));
        }
        
        long count = notificationService.getUnreadCount(currentUser.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        response.put("userId", currentUser.getId());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/admin")
    public ResponseEntity<?> getAdminNotifications(
            @RequestParam(defaultValue = "20") int limit) {
        
        User currentUser = AuthInterceptor.getCurrentUser();
        if (currentUser == null || !currentUser.isAdmin()) {
            return ResponseEntity.status(403).body(createErrorResponse("Admin access required"));
        }
        
        List<Notification> notifications = notificationService.getAdminNotifications(limit);
        return ResponseEntity.ok(notifications);
    }
    
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        User currentUser = AuthInterceptor.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(createErrorResponse("Unauthorized"));
        }
        
        try {
            notificationService.markAsRead(id, currentUser.getId());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Notification marked as read");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(createErrorResponse(e.getMessage()));
        }
    }
    
    @PutMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead() {
        User currentUser = AuthInterceptor.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(createErrorResponse("Unauthorized"));
        }
        
        notificationService.markAllAsRead(currentUser.getId());
        Map<String, String> response = new HashMap<>();
        response.put("message", "All notifications marked as read");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getOrderNotifications(@PathVariable String orderId) {
        User currentUser = AuthInterceptor.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(createErrorResponse("Unauthorized"));
        }
        
        // Check if user has access to this order
        // (You'll need to implement order access check based on your business logic)
        
        List<Notification> notifications = notificationService.getOrderNotifications(orderId);
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<?> getNotificationStatistics(
            @RequestParam(defaultValue = "7") int days) {
        
        User currentUser = AuthInterceptor.getCurrentUser();
        if (currentUser == null || !currentUser.isAdmin()) {
            return ResponseEntity.status(403).body(createErrorResponse("Admin access required"));
        }
        
        List<Object[]> stats = notificationService.getNotificationStatistics(
                java.time.LocalDateTime.now().minusDays(days));
        
        Map<String, Object> response = new HashMap<>();
        response.put("periodDays", days);
        response.put("statistics", stats);
        return ResponseEntity.ok(response);
    }
    
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}