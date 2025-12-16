package com.IT_market.controller;

import com.IT_market.dto.GuestOrderRequest;
import com.IT_market.model.Order;
import com.IT_market.model.User;
import com.IT_market.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private final OrderService orderService;
    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    @PostMapping("/guest")
    public ResponseEntity<?> createGuestOrder(@Valid @RequestBody GuestOrderRequest request) {
        try {
            Order order = orderService.createGuestOrder(request);
            System.out.println("Guest order created: " + order.getId() + " for email: " + order.getCustomerEmail());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
            
        } catch (Exception e) {
            System.err.println("Error creating guest order: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create order");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(
            @PathVariable String id,
            @RequestParam(required = false) String email) {
        
        try {
            Order order = orderService.getOrderById(id);
            
            // For guest orders, verify email matches
            if (order.isGuestOrder()) {
                if (email == null || !order.getCustomerEmail().equals(email)) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Forbidden");
                    error.put("message", "Email verification required for guest orders");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
                }
            }
            
            return ResponseEntity.ok(order);
            
        } catch (RuntimeException e) {
            System.err.println("Order not found: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Order not found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    @GetMapping("/by-email")
    public ResponseEntity<?> getOrdersByEmail(@RequestParam String email) {
        try {
            List<Order> orders = orderService.getOrdersByEmail(email);
            return ResponseEntity.ok(orders);
            
        } catch (Exception e) {
            System.err.println("Error fetching orders by email: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch orders");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}