package com.IT_market.service;

import com.IT_market.dto.GuestOrderRequest;
import com.IT_market.model.*;
import com.IT_market.repository.OrderRepository;
import com.IT_market.repository.ProductRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
    

    @Transactional(readOnly = true)
    public long countAllOrders() {
        return orderRepository.count();
    }
    @Transactional(readOnly = true)
    public List<Order> getOrderByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public long countOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)

    public List<Order> getRecentOrders(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")); // ✅ Correct
        return orderRepository.findAll(pageable).getContent();
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getSalesSummary(LocalDate startDate, LocalDate endDate) {
        Map<String, BigDecimal> salesData = new LinkedHashMap<>();

        // Get orders within date range
        List<Order> orders = orderRepository.findByOrderDateBetween(
            startDate.atStartOfDay(),
            endDate.atTime(23, 59, 59)
        );

        // Group by day
        for (Order order : orders) {
            if (order.getStatus() == Order.OrderStatus.DELIVERED || 
                order.getStatus() == Order.OrderStatus.PAID) {
                String dateKey = order.getCreatedAt().toLocalDate().toString();
                BigDecimal currentAmount = salesData.getOrDefault(dateKey, BigDecimal.ZERO);
                salesData.put(dateKey, currentAmount.add(order.getTotalAmount()));
            }
        }

        // Fill missing dates with zero
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String dateKey = currentDate.toString();
            salesData.putIfAbsent(dateKey, BigDecimal.ZERO);
            currentDate = currentDate.plusDays(1);
        }

        return salesData;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSalesReport(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> report = new HashMap<>();

        // Get all orders in date range
        List<Order> orders = orderRepository.findByOrderDateBetween(
            startDate.atStartOfDay(),
            endDate.atTime(23, 59, 59)
        );

        // Calculate totals
        BigDecimal totalSales = orders.stream()
            .filter(order -> order.getStatus() == Order.OrderStatus.DELIVERED || 
                            order.getStatus() == Order.OrderStatus.PAID)
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrdersCount = orders.size();
        long completedOrders = orders.stream()
            .filter(order -> order.getStatus() == Order.OrderStatus.DELIVERED)
            .count();

        BigDecimal averageOrderValue = totalOrdersCount > 0 ? 
            totalSales.divide(BigDecimal.valueOf(totalOrdersCount), 2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO;

        // Popular payment methods
        Map<String, Long> paymentMethodCounts = orders.stream()
            .collect(Collectors.groupingBy(
                order -> order.getPaymentMethod().name(),
                Collectors.counting()
            ));

        report.put("totalSales", totalSales);
        report.put("totalOrders", totalOrdersCount);
        report.put("completedOrders", completedOrders);
        report.put("averageOrderValue", averageOrderValue);
        report.put("paymentMethodDistribution", paymentMethodCounts);
        report.put("startDate", startDate);
        report.put("endDate", endDate);

        return report;
    }
    
    // Add these methods to your OrderService class

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    @Transactional
    public void approveOrder(String id) {
        Order order = getOrderById(id.toString()); // Note: Your order ID is String
        order.setStatus(Order.OrderStatus.CONFIRMED);
        //order.setApprovedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Send notification
        notificationService.createOrderStatusNotification(order);
    }

    @Transactional
    public void rejectOrder(Long id, String reason) {
        Order order = getOrderById(id.toString()); // Note: Your order ID is String
        order.setStatus(Order.OrderStatus.REJECTED);
        //order.setCancelledAt(LocalDateTime.now());
        //order.setCancellationReason(reason);
        orderRepository.save(order);

        // Send notification
        notificationService.createOrderStatusNotification(order);
    }


    // Helper method to get order by Long id (converting to String)
    private Order getOrderById(Long id) {
        // This assumes you have a way to convert Long id to your String order ID
        // If not, you might need to store Long IDs separately
        return getOrderById(id.toString());
    }


}