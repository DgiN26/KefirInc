package com.kefir.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaymentCartsRepository extends JpaRepository<PaymentCarts, Integer> {
    Optional<PaymentCarts> findByIdUsers(Long idUsers);
    Optional<PaymentCarts> findByCartNumber(String cartNumber);
    boolean existsByIdUsers(Long idUsers);
    boolean existsByCartNumber(String cartNumber);
}