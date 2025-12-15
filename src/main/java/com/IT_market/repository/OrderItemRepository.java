
package com.IT_market.repository;

import com.IT_market.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;


public interface OrderItemRepository extends JpaRepository<OrderItem, Long>  {
    
}
