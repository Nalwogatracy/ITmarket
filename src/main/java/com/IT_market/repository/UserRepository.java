
package com.IT_market.repository;

import com.IT_market.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email);
    
}
