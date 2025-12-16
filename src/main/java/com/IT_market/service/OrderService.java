package com.IT_market.service;

import com.IT_market.dto.GuestOrderRequest;
import com.IT_market.model.*;
import com.IT_market.repository.OrderRepository;
import com.IT_market.repository.ProductRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    
    private final DateTimeFormatter orderIdFormatter = 
            DateTimeFormatter.ofPattern("yyMMddHHmmss");
    
    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                       NotificationService notificationService, EmailService emailService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }
    
    @Transactional
    public Order createGuestOrder(GuestOrderRequest request) {
        Order order = buildGuestOrder(request);
        order = orderRepository.save(order);
        
        // Update product stock
        updateProductStock(order);
        
        // Send notifications asynchronously
        sendOrderNotifications(order);
        
        return order;
    }
    
    @Transactional
    public Order createUserOrder(com.IT_market.dto.OrderRequest request, User user) {
        Order order = buildUserOrder(request, user);
        order = orderRepository.save(order);
        
        // Update product stock
        updateProductStock(order);
        
        // Send notifications asynchronously
        sendOrderNotifications(order);
        
        return order;
    }
    
    private Order buildGuestOrder(GuestOrderRequest request) {
        // Calculate order items and total
        BigDecimal subtotal = BigDecimal.ZERO;
        
        // Create order ID
        String orderId = "TS-" + LocalDateTime.now().format(orderIdFormatter) + 
                         "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Order order = new Order();
        order.setId(orderId);
        order.setCustomerEmail(request.getEmail());
        order.setCustomerPhone(request.getPhone());
        order.setCustomerName(request.getName());
        order.setShippingAddress(request.getShippingAddress());
        order.setNotes(request.getNotes());
        order.setGuestOrder(true);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        order.setPaymentMethod(Order.PaymentMethod.CREDIT_CARD);
        
        // Note: In real implementation, you would add order items here
        // For now, set default values
        order.setSubtotal(subtotal);
        order.setTotalAmount(subtotal);
        
        return order;
    }
    
    private Order buildUserOrder(com.IT_market.dto.OrderRequest request, User user) {
        // Similar to buildGuestOrder but with user
        String orderId = "TS-" + LocalDateTime.now().format(orderIdFormatter) + 
                         "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Order order = new Order();
        order.setId(orderId);
        order.setUser(user);
        order.setCustomerEmail(user.getEmail());
        order.setCustomerPhone(user.getPhoneNumber());
        order.setCustomerName(user.getFullName());
        order.setShippingAddress(request.getShippingAddress());
        order.setNotes(request.getNotes());
        order.setGuestOrder(false);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        order.setPaymentMethod(request.getPaymentMethod());
        
        // Calculate totals from request items
        BigDecimal subtotal = calculateSubtotal(request);
        order.setSubtotal(subtotal);
        order.setTaxAmount(request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO);
        order.setShippingAmount(request.getShippingAmount() != null ? request.getShippingAmount() : BigDecimal.ZERO);
        order.setDiscountAmount(request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO);
        
        BigDecimal total = subtotal
                .add(order.getTaxAmount())
                .add(order.getShippingAmount())
                .subtract(order.getDiscountAmount());
        
        order.setTotalAmount(total);
        
        return order;
    }
    
    private BigDecimal calculateSubtotal(com.IT_market.dto.OrderRequest request) {
        BigDecimal subtotal = BigDecimal.ZERO;
        // Calculate from items
        return subtotal;
    }
    
    private void updateProductStock(Order order) {
        // Update stock for each product in order
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int newStock = product.getStockQuantity() - item.getQuantity();
            
            if (newStock < 0) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }
            
            product.setStockQuantity(newStock);
            product.setSoldCount(product.getSoldCount() + item.getQuantity());
            productRepository.save(product);
            
            // Check for low stock notification
            if (newStock < 10) {
                notificationService.createLowStockNotification(product);
            }
        }
    }
    
    @Async
    protected void sendOrderNotifications(Order order) {
        try {
            // 1. Create admin notification
            notificationService.createOrderNotification(order);
            
            // 2. Send email to admin
            emailService.sendOrderNotificationToAdmin(order);
            
            // 3. Send confirmation email to customer
            emailService.sendOrderConfirmation(order);
            
            // 4. Update order as notified
            order.setAdminNotified(true);
            order.setNotificationSentAt(LocalDateTime.now());
            orderRepository.save(order);
            
            System.out.println("Order notifications sent successfully for order: " + order.getId());
            
        } catch (Exception e) {
            System.err.println("Failed to send order notifications for order: " + order.getId());
            e.printStackTrace();
        }
    }
    
    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }
    
    public List<Order> getOrdersByEmail(String email) {
        return orderRepository.findByCustomerEmail(email);
    }
    
    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }
    
    @Transactional
    public Order updateOrderStatus(String orderId, Order.OrderStatus status) {
        Order order = getOrderById(orderId);
        order.setStatus(status);
        
        if (status == Order.OrderStatus.DELIVERED) {
            order.setCompletedAt(LocalDateTime.now());
        }
        
        order = orderRepository.save(order);
        
        // Send status update notification
        notificationService.createOrderStatusNotification(order);
        
        return order;
    }
}