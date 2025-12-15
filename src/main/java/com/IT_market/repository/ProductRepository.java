
package com.IT_market.repository;

import com.IT_market.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ProductRepository extends JpaRepository<Product, Long> {
    
}
