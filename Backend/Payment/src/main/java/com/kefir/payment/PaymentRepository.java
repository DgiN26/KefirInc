package com.kefir.payment;

import com.kefir.payment.PaymentAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentAccount, Long> {

    Optional<PaymentAccount> findByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE PaymentAccount p SET p.cash = 0 WHERE p.userId = :userId")
    int clearCashByUserId(Long userId);

    @Query("SELECT p FROM PaymentAccount p WHERE p.userId = -1")
    Optional<PaymentAccount> findSystemAccount();

    boolean existsByUserId(Long userId);
}