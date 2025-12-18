// File: src/main/java/com/IT_market/repository/SellRequestRepository.java
package com.IT_market.repository;

import com.IT_market.model.SellRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SellRequestRepository extends JpaRepository<SellRequest, Long> {
    
    // Find by status
    List<SellRequest> findByStatus(SellRequest.SellRequestStatus status);
    Page<SellRequest> findByStatus(SellRequest.SellRequestStatus status, Pageable pageable);
    
    // Find by user
    List<SellRequest> findByUserId(Long userId);
    Page<SellRequest> findByUserId(Long userId, Pageable pageable);
    
    // Find pending requests (for admin dashboard)
    List<SellRequest> findByStatusOrderBySubmittedAtAsc(SellRequest.SellRequestStatus status);
    
    // Count by status
    long countByStatus(SellRequest.SellRequestStatus status);
    
    // Find requests within date range
    @Query("SELECT sr FROM SellRequest sr WHERE sr.submittedAt BETWEEN :startDate AND :endDate")
    List<SellRequest> findBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
    
    // Find requests with expected price range
    List<SellRequest> findByExpectedPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    
    // Find by category
    List<SellRequest> findByItemCategory(String category);
    
    // Get statistics
    @Query("SELECT COUNT(sr) FROM SellRequest sr WHERE sr.status = 'PENDING'")
    long countPendingRequests();
    
    @Query("SELECT SUM(sr.finalPrice) FROM SellRequest sr WHERE sr.status = 'COMPLETED'")
    BigDecimal getTotalCompletedValue();
    
    @Query("SELECT AVG(sr.finalPrice) FROM SellRequest sr WHERE sr.status = 'COMPLETED'")
    Double getAverageCompletedPrice();
    
    // Find recent requests (for admin dashboard)
    @Query("SELECT sr FROM SellRequest sr ORDER BY sr.submittedAt DESC")
    List<SellRequest> findRecentRequests(Pageable pageable);
    
    // Find by status and user
    List<SellRequest> findByUserIdAndStatus(Long userId, SellRequest.SellRequestStatus status);
    
    // Check if user has pending request for same item (avoid duplicates)
    @Query("SELECT COUNT(sr) > 0 FROM SellRequest sr WHERE sr.user.id = :userId AND sr.itemName = :itemName AND sr.status IN ('PENDING', 'UNDER_REVIEW', 'NEGOTIATING')")
    boolean existsActiveRequestForUserAndItem(@Param("userId") Long userId, @Param("itemName") String itemName);
}