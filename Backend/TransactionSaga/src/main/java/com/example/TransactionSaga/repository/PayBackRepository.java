package com.example.TransactionSaga.repository;

import com.example.TransactionSaga.entity.PayBack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayBackRepository extends JpaRepository<PayBack, Long> {
    boolean existsByCartId(Integer cartId);
}