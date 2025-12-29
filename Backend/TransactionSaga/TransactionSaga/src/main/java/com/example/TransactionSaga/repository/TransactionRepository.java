package com.example.TransactionSaga.repository;

import com.example.TransactionSaga.model.entity.TransactionEntity;
import com.example.TransactionSaga.model.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

    // Основные поиски
    Optional<TransactionEntity> findByOrderId(String orderId);

    List<TransactionEntity> findByCollectorId(String collectorId);

    List<TransactionEntity> findByClientId(String clientId);

    List<TransactionEntity> findByStatus(TransactionStatus status);

    // Поиск по нескольким статусам
    @Query("SELECT t FROM TransactionEntity t WHERE t.status IN :statuses ORDER BY t.createdAt DESC")
    List<TransactionEntity> findByStatusIn(@Param("statuses") List<TransactionStatus> statuses);

    // Поиск активных транзакций сборщика
    @Query("SELECT t FROM TransactionEntity t WHERE t.collectorId = :collectorId AND t.status IN :activeStatuses")
    List<TransactionEntity> findActiveByCollectorId(
            @Param("collectorId") String collectorId,
            @Param("activeStatuses") List<TransactionStatus> activeStatuses);

    // Поиск транзакций с проблемами
    @Query("SELECT t FROM TransactionEntity t WHERE t.status = 'WAITING_CLIENT' OR t.status = 'WAITING_OFFICE'")
    List<TransactionEntity> findProblemTransactions();

    // Поиск просроченных транзакций
    @Query("SELECT t FROM TransactionEntity t WHERE t.status = :status AND t.clientNotifiedAt < :threshold")
    List<TransactionEntity> findTimeoutTransactions(
            @Param("status") TransactionStatus status,
            @Param("threshold") LocalDateTime threshold);

    // Статистика по коллектору
    @Query("SELECT COUNT(t) FROM TransactionEntity t WHERE t.collectorId = :collectorId AND t.status = 'ACTIVE'")
    long countActiveTransactionsByCollector(@Param("collectorId") String collectorId);

    @Query("SELECT COUNT(t) FROM TransactionEntity t WHERE t.collectorId = :collectorId AND t.status IN ('COMPLETED', 'CANCELLED')")
    long countCompletedTransactionsByCollector(@Param("collectorId") String collectorId);

    // История заказов
    @Query("SELECT t FROM TransactionEntity t WHERE t.orderId = :orderId ORDER BY t.createdAt DESC")
    List<TransactionEntity> findHistoryByOrderId(@Param("orderId") String orderId);

    // Поиск транзакций с отсутствующими товарами
    @Query("SELECT t FROM TransactionEntity t WHERE SIZE(t.missingItems) > 0")
    List<TransactionEntity> findTransactionsWithMissingItems();

    // Обновление статуса транзакции
    @Modifying
    @Transactional
    @Query("UPDATE TransactionEntity t SET t.status = :newStatus, t.updatedAt = CURRENT_TIMESTAMP WHERE t.id = :transactionId")
    int updateTransactionStatus(
            @Param("transactionId") String transactionId,
            @Param("newStatus") TransactionStatus newStatus);

    // Обновление статуса с причиной
    @Modifying
    @Transactional
    @Query("UPDATE TransactionEntity t SET t.status = :newStatus, t.pauseReason = :reason, t.updatedAt = CURRENT_TIMESTAMP WHERE t.id = :transactionId")
    int pauseTransaction(
            @Param("transactionId") String transactionId,
            @Param("newStatus") TransactionStatus newStatus,
            @Param("reason") String reason);

    // Добавление отсканированного товара
    @Modifying
    @Transactional
    @Query(value = "UPDATE transactions SET scanned_items = scanned_items || jsonb_build_object(:productId, :quantity), updated_at = CURRENT_TIMESTAMP WHERE id = :transactionId", nativeQuery = true)
    int addScannedItem(
            @Param("transactionId") String transactionId,
            @Param("productId") String productId,
            @Param("quantity") Integer quantity);

    // Добавление отсутствующего товара
    @Modifying
    @Transactional
    @Query(value = "UPDATE transactions SET missing_items = missing_items || jsonb_build_object(:productId, :quantity), updated_at = CURRENT_TIMESTAMP WHERE id = :transactionId", nativeQuery = true)
    int addMissingItem(
            @Param("transactionId") String transactionId,
            @Param("productId") String productId,
            @Param("quantity") Integer quantity);

    // Удаление отсутствующего товара
    @Modifying
    @Transactional
    @Query(value = "UPDATE transactions SET missing_items = missing_items - :productId, updated_at = CURRENT_TIMESTAMP WHERE id = :transactionId", nativeQuery = true)
    int removeMissingItem(
            @Param("transactionId") String transactionId,
            @Param("productId") String productId);

    // Поиск транзакций по временному диапазону
    @Query("SELECT t FROM TransactionEntity t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    List<TransactionEntity> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Проверка существования активной транзакции для заказа
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM TransactionEntity t WHERE t.orderId = :orderId AND t.status IN ('CREATED', 'ACTIVE', 'PAUSED', 'WAITING_CLIENT', 'WAITING_OFFICE')")
    boolean existsActiveTransactionForOrder(@Param("orderId") String orderId);

    // Поиск последней транзакции для заказа
    @Query("SELECT t FROM TransactionEntity t WHERE t.orderId = :orderId ORDER BY t.createdAt DESC LIMIT 1")
    Optional<TransactionEntity> findLatestByOrderId(@Param("orderId") String orderId);
}