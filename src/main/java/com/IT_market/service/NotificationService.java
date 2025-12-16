package com.IT_market.service;

import com.IT_market.model.*;
import com.IT_market.repository.NotificationRepository;
import com.IT_market.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    public NotificationService(NotificationRepository notificationRepository,
                              UserRepository userRepository,
                              SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }
    
    @Transactional
    public List<Notification> createOrderNotification(Order order) {
        String title = "New Order Received";
        String message = String.format("Order #%s from %s for $%.2f", 
                order.getId(), 
                order.getCustomerEmail(), 
                order.getTotalAmount());
        
        List<Notification> createdNotifications = new ArrayList<>();
        
        // Find admin users
        List<User> admins = userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName() == Role.RoleName.ROLE_ADMIN))
                .collect(Collectors.toList());
        
        for (User admin : admins) {
            Notification notification = new Notification();
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setType(Notification.NotificationType.ORDER_CREATED);
            notification.setStatus(Notification.NotificationStatus.UNREAD);
            notification.setRelatedEntityId(order.getId());
            notification.setRelatedEntityType(Notification.EntityType.ORDER);
            notification.setRecipient(admin);
            
            Notification savedNotification = notificationRepository.save(notification);
            createdNotifications.add(savedNotification);
            
            // Send real-time WebSocket notification
            sendWebSocketNotification(admin.getId(), savedNotification);
        }
        
        // Also create a general admin notification (for admins who login later)
        Notification generalNotification = new Notification();
        generalNotification.setTitle(title);
        generalNotification.setMessage(message);
        generalNotification.setType(Notification.NotificationType.ORDER_CREATED);
        generalNotification.setStatus(Notification.NotificationStatus.UNREAD);
        generalNotification.setRelatedEntityId(order.getId());
        generalNotification.setRelatedEntityType(Notification.EntityType.ORDER);
        // No recipient means it's for all admins
        generalNotification.setRecipient(null);
        
        Notification savedGeneralNotification = notificationRepository.save(generalNotification);
        createdNotifications.add(savedGeneralNotification);
        
        System.out.println("Order notification created for " + (admins.size() + 1) + " admin records");
        return createdNotifications;
    }
    
    @Transactional
    public List<Notification> createLowStockNotification(Product product) {
        String title = "Low Stock Alert";
        String message = String.format("Product '%s' is running low. Current stock: %d", 
                product.getName(), product.getStockQuantity());
        
        List<Notification> createdNotifications = new ArrayList<>();
        
        // Find admins
        List<User> admins = userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName() == Role.RoleName.ROLE_ADMIN))
                .collect(Collectors.toList());
        
        // Add product seller if exists
        List<User> recipients = new ArrayList<>(admins);
        if (product.getSeller() != null) {
            recipients.add(product.getSeller());
        }
        
        for (User recipient : recipients) {
            Notification notification = new Notification();
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setType(Notification.NotificationType.LOW_STOCK);
            notification.setStatus(Notification.NotificationStatus.UNREAD);
            notification.setRelatedEntityId(product.getId());
            notification.setRelatedEntityType(Notification.EntityType.PRODUCT);
            notification.setRecipient(recipient);
            
            Notification savedNotification = notificationRepository.save(notification);
            createdNotifications.add(savedNotification);
            
            sendWebSocketNotification(recipient.getId(), savedNotification);
        }
        
        System.out.println("Low stock notification created for " + recipients.size() + " recipients");
        return createdNotifications;
    }
    
    @Transactional
    public Notification createOrderStatusNotification(Order order) {
        String title = "Order Status Updated";
        String message = String.format("Order #%s status changed to %s", 
                order.getId(), order.getStatus());
        
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(Notification.NotificationType.ORDER_UPDATED);
        notification.setStatus(Notification.NotificationStatus.UNREAD);
        notification.setRelatedEntityId(order.getId());
        notification.setRelatedEntityType(Notification.EntityType.ORDER);
        
        if (order.getUser() != null) {
            // For registered users, notify them
            notification.setRecipient(order.getUser());
            Notification savedNotification = notificationRepository.save(notification);
            sendWebSocketNotification(order.getUser().getId(), savedNotification);
            return savedNotification;
        } else {
            // For guest orders, notify all admins
            notification.setRecipient(null); // Null means it's for all admins
            Notification savedNotification = notificationRepository.save(notification);
            
            // Also send to individual admins via WebSocket
            List<User> admins = userRepository.findAll().stream()
                    .filter(user -> user.getRoles().stream()
                            .anyMatch(role -> role.getName() == Role.RoleName.ROLE_ADMIN))
                    .collect(Collectors.toList());
            
            for (User admin : admins) {
                sendWebSocketNotification(admin.getId(), savedNotification);
            }
            
            return savedNotification;
        }
    }
    
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable)
                .getContent();
    }
    
    @Transactional(readOnly = true)
    public List<Notification> getAdminNotifications(int limit) {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        Pageable pageable = PageRequest.of(0, limit);
        return notificationRepository.findRecentNotifications(oneWeekAgo, pageable)
                .getContent();
    }
    
    @Transactional(readOnly = true)
    public List<Notification> getRecentNotifications(int limit) {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        return notificationRepository.findRecentNotifications(oneWeekAgo);
    }
    
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }
    
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        if (notification.getRecipient() != null && 
            !notification.getRecipient().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to mark this notification as read");
        }
        
        notification.setStatus(Notification.NotificationStatus.READ);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }
    
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findUnreadByUserId(userId);
        for (Notification notification : unreadNotifications) {
            notification.setStatus(Notification.NotificationStatus.READ);
            notification.setReadAt(LocalDateTime.now());
        }
        notificationRepository.saveAll(unreadNotifications);
    }
    
    @Transactional
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void cleanupOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        notificationRepository.deleteOldNotifications(cutoff);
        System.out.println("Old notifications cleanup completed");
    }
    
    @Transactional(readOnly = true)
    public List<Notification> getOrderNotifications(String orderId) {
        return notificationRepository.findByOrderId(orderId);
    }
    
    @Transactional(readOnly = true)
    public List<Notification> getProductNotifications(String productId) {
        return notificationRepository.findByProductId(productId);
    }
    
    @Transactional(readOnly = true)
    public List<Object[]> getNotificationStatistics(LocalDateTime since) {
        return notificationRepository.countByTypeSince(since);
    }
    
    private void sendWebSocketNotification(Long userId, Notification notification) {
        try {
            String destination = "/topic/notifications/" + userId;
            messagingTemplate.convertAndSend(destination, notification);
            System.out.println("WebSocket notification sent to user " + userId + ": " + notification.getTitle());
        } catch (Exception e) {
            System.err.println("Failed to send WebSocket notification: " + e.getMessage());
        }
    }
}