package com.IT_market.repository;

import com.IT_market.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    Page<Product> findByActiveTrue(Pageable pageable);
    Page<Product> findByFeaturedTrueAndActiveTrue(Pageable pageable);
    Page<Product> findByCategoryAndActiveTrue(String category, Pageable pageable);
    Page<Product> findByBrandAndActiveTrue(String brand, Pageable pageable);
    Page<Product> findBySellerId(Long sellerId, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.brand) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "EXISTS (SELECT t FROM p.tags t WHERE LOWER(t) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<Product> searchProducts(@Param("search") String search,
                                 @Param("category") String category,
                                 @Param("minPrice") BigDecimal minPrice,
                                 @Param("maxPrice") BigDecimal maxPrice,
                                 Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stockQuantity < :threshold")
    List<Product> findLowStockProducts(@Param("threshold") Integer threshold);
    
    List<Product> findByStockQuantityLessThan(Integer quantity);
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true")
    Long countActiveProducts();
    
    @Query("SELECT SUM(p.soldCount) FROM Product p")
    Long totalProductsSold();
    
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.viewCount = COALESCE(p.viewCount, 0) + 1 WHERE p.id = :productId")
    void incrementViewCount(@Param("productId") String productId);
    
    // Get most viewed products
    @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.viewCount DESC NULLS LAST")
    Page<Product> findMostViewedProducts(Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.createdAt >= CURRENT_DATE - :days ORDER BY p.viewCount DESC NULLS LAST")
    Page<Product> findTrendingProducts(@Param("days") int days, Pageable pageable);
    
    @Query("SELECT SUM(p.viewCount) FROM Product p")
    Long getTotalViews();
    
    @Query("SELECT AVG(p.viewCount) FROM Product p WHERE p.viewCount > 0")
    Double getAverageViews();
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.viewCount >= :threshold")
    List<Product> findPopularProducts(@Param("threshold") int threshold);
}