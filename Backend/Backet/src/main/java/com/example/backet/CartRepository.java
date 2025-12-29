package com.example.backet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    List<Cart> findByClientId(int clientId);
    Cart findByClientIdAndStatus(int clientId, String status);

}
