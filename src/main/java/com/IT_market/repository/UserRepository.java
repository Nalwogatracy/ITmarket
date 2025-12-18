package com.IT_market.repository;

import com.IT_market.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    Optional<User> findByIdAndActiveTrue(Long id);
    boolean existsByUsernameAndActiveTrue(String username);
    boolean existsByEmailAndActiveTrue(String email);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= ?1")
    Long countByCreatedAtAfter(LocalDateTime date);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.business = true")
    Long countBusinessUsers();
    
    
}