
package com.IT_market.repository;

import com.IT_market.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;


public interface OrderRepository extends JpaRepository<Order, Long>{
    
}
