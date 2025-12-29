// OrderRepository.java
package com.example.backet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByCartId(int cartId);
    Order findByOrderNumber(String orderNumber);
    List<Order> findByStatus(String status);

}