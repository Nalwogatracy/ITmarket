package com.IT_market.repository;

import com.IT_market.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Basic queries
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    
    // CORRECTED: All methods should use "Enabled" consistently
    // Method 1: Using @Query to map old method names to new field
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.enabled = true")
    Optional<User> findByIdAndActiveTrue(@Param("id") Long id);
    
    // Method 2: Spring Data JPA auto-generated query
    Optional<User> findByIdAndEnabledTrue(Long id);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.username = :username AND u.enabled = true")
    boolean existsByUsernameAndActiveTrue(@Param("username") String username);
    
    // Spring Data JPA auto-generated version
    boolean existsByUsernameAndEnabledTrue(String username);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email AND u.enabled = true")
    boolean existsByEmailAndActiveTrue(@Param("email") String email);
    
    // Spring Data JPA auto-generated version
    boolean existsByEmailAndEnabledTrue(String email);
    
    // Other queries
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :date")
    Long countByCreatedAtAfter(@Param("date") LocalDateTime date);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.businessAccount = true")
    Long countBusinessUsers();
    
    // Add these Spring Data JPA methods for consistency
    long countByEnabledTrue();
    long countByEnabledFalse();
}