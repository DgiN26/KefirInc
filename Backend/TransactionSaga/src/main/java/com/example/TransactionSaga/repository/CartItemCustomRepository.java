package com.example.TransactionSaga.repository;

import com.example.TransactionSaga.dto.CartItemVozvratProjection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CartItemCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<CartItemVozvratProjection> findVozvratTcRecords(int limit) {
        String sql = """
            SELECT 
                ci.id,
                ci.cart_id,
                ci.price,
                c.client_id,
                c.created_date
            FROM cart_items ci
            JOIN carts c ON ci.cart_id = c.id
            WHERE ci.vozvrat = 'tc'
            ORDER BY ci.id
            LIMIT :limit
            """;

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(sql)
                .setParameter("limit", limit)
                .getResultList();

        return results.stream()
                .map(row -> new CartItemVozvratProjection() {
                    @Override
                    public Long getId() {
                        return row[0] != null ? ((Number) row[0]).longValue() : null;
                    }

                    @Override
                    public Integer getCartId() {
                        return row[1] != null ? ((Number) row[1]).intValue() : null;
                    }

                    @Override
                    public BigDecimal getPrice() {
                        return row[2] != null ? new BigDecimal(row[2].toString()) : null;
                    }

                    @Override
                    public Long getClientId() {
                        return row[3] != null ? Long.valueOf(row[3].toString()) : null;
                    }

                    @Override
                    public String getStatus() {
                        return "created";
                    }

                    @Override
                    public java.time.LocalDateTime getCreatedDate() {
                        if (row[4] == null) return null;
                        if (row[4] instanceof java.sql.Timestamp) {
                            return ((java.sql.Timestamp) row[4]).toLocalDateTime();
                        }
                        return (java.time.LocalDateTime) row[4];
                    }
                })
                .collect(Collectors.toList());
    }
}