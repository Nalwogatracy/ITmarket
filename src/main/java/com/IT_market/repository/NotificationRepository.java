package com.IT_market.repository;

import com.IT_market.model.Notification;
import com.IT_market.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Find notifications for a specific user, ordered by creation date (newest first)
    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
    
    // Find unread notifications for a specific user
    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId AND n.status = 'UNREAD'")
    List<Notification> findUnreadByUserId(@Param("userId") Long userId);
    
    // Find unread notifications for a specific user with pagination
    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId AND n.status = 'UNREAD'")
    Page<Notification> findUnreadByUserId(@Param("userId") Long userId, Pageable pageable);
    
    // Count unread notifications for a specific user
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipient.id = :userId AND n.status = 'UNREAD'")
    Long countUnreadByUserId(@Param("userId") Long userId);
    
    // Find notifications created after a specific date
    @Query("SELECT n FROM Notification n WHERE n.createdAt >= :date ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(@Param("date") LocalDateTime date);
    
    // Find notifications created after a specific date with pagination
    @Query("SELECT n FROM Notification n WHERE n.createdAt >= :date ORDER BY n.createdAt DESC")
    Page<Notification> findRecentNotifications(@Param("date") LocalDateTime date, Pageable pageable);
    
    // Find admin notifications (where recipient is null or specific admin users)
    @Query("SELECT n FROM Notification n WHERE n.recipient IS NULL OR n.recipient IN (SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_ADMIN')")
    List<Notification> findAdminNotifications();
    
    // Find admin notifications created after a specific date
    @Query("SELECT n FROM Notification n WHERE (n.recipient IS NULL OR n.recipient IN (SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_ADMIN')) AND n.createdAt >= :date")
    List<Notification> findAdminNotificationsSince(@Param("date") LocalDateTime date);
    
    // Find notifications by type
    @Query("SELECT n FROM Notification n WHERE n.type = :type ORDER BY n.createdAt DESC")
    List<Notification> findByType(@Param("type") Notification.NotificationType type);
    
    // Find notifications by type and recipient
    @Query("SELECT n FROM Notification n WHERE n.type = :type AND n.recipient.id = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByTypeAndRecipient(@Param("type") Notification.NotificationType type, 
                                              @Param("userId") Long userId);
    
    // Find notifications by related entity
    @Query("SELECT n FROM Notification n WHERE n.relatedEntityId = :entityId AND n.relatedEntityType = :entityType ORDER BY n.createdAt DESC")
    List<Notification> findByRelatedEntity(@Param("entityId") String entityId, 
                                           @Param("entityType") Notification.EntityType entityType);
    
    // Find order-related notifications for a specific order
    @Query("SELECT n FROM Notification n WHERE n.relatedEntityId = :orderId AND n.relatedEntityType = 'ORDER' ORDER BY n.createdAt DESC")
    List<Notification> findByOrderId(@Param("orderId") String orderId);
    
    // Find product-related notifications for a specific product
    @Query("SELECT n FROM Notification n WHERE n.relatedEntityId = :productId AND n.relatedEntityType = 'PRODUCT' ORDER BY n.createdAt DESC")
    List<Notification> findByProductId(@Param("productId") String productId);
    
    // Find notifications that need to be sent (not yet sent)
    @Query("SELECT n FROM Notification n WHERE n.status = 'UNREAD' AND n.createdAt >= :since")
    List<Notification> findUnsentNotifications(@Param("since") LocalDateTime since);
    
    // Bulk mark notifications as read
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = CURRENT_TIMESTAMP WHERE n.id IN :ids")
    void markAsRead(@Param("ids") List<Long> ids);
    
    // Delete old notifications (older than specified date)
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoff")
    void deleteOldNotifications(@Param("cutoff") LocalDateTime cutoff);
    
    // Find notifications for admin dashboard (recent, important notifications)
    @Query("SELECT n FROM Notification n WHERE n.type IN ('ORDER_CREATED', 'LOW_STOCK', 'PAYMENT_RECEIVED') ORDER BY n.createdAt DESC")
    Page<Notification> findImportantNotifications(Pageable pageable);
    
    // Statistics: Count notifications by type
    @Query("SELECT n.type, COUNT(n) FROM Notification n WHERE n.createdAt >= :since GROUP BY n.type")
    List<Object[]> countByTypeSince(@Param("since") LocalDateTime since);
    
    // Statistics: Count notifications by status
    @Query("SELECT n.status, COUNT(n) FROM Notification n WHERE n.createdAt >= :since GROUP BY n.status")
    List<Object[]> countByStatusSince(@Param("since") LocalDateTime since);
    
    // Custom method to find notifications for a user with specific types
    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId AND n.type IN :types ORDER BY n.createdAt DESC")
    List<Notification> findByRecipientAndTypes(@Param("userId") Long userId, 
                                               @Param("types") List<Notification.NotificationType> types);
}