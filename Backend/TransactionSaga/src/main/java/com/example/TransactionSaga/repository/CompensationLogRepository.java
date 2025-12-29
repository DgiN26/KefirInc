package com.example.TransactionSaga.repository;

import com.example.TransactionSaga.model.entity.CompensationLogEntity;
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
public interface CompensationLogRepository extends JpaRepository<CompensationLogEntity, Long> {

    // Поиск по ID транзакции
    List<CompensationLogEntity> findByTransactionId(String transactionId);

    List<CompensationLogEntity> findByTransactionIdOrderByCreatedAtDesc(String transactionId);

    // Поиск по статусу
    List<CompensationLogEntity> findByStatus(String status);

    List<CompensationLogEntity> findByTransactionIdAndStatus(String transactionId, String status);

    // Поиск по типу компенсации
    List<CompensationLogEntity> findByCompensationType(String compensationType);

    // Поиск по шагу саги
    Optional<CompensationLogEntity> findBySagaStepId(Long sagaStepId);

    // Поиск неудачных компенсаций для повторной попытки
    @Query("SELECT c FROM CompensationLogEntity c WHERE c.status = 'FAILED' AND c.retryCount < 3")
    List<CompensationLogEntity> findFailedCompensationsForRetry();

    // Поиск активных компенсаций
    @Query("SELECT c FROM CompensationLogEntity c WHERE c.status IN ('PENDING', 'IN_PROGRESS')")
    List<CompensationLogEntity> findActiveCompensations();

    // Статистика по компенсациям
    @Query("SELECT COUNT(c) FROM CompensationLogEntity c WHERE c.transactionId = :transactionId")
    long countByTransactionId(@Param("transactionId") String transactionId);

    @Query("SELECT COUNT(c) FROM CompensationLogEntity c WHERE c.transactionId = :transactionId AND c.status = 'COMPLETED'")
    long countCompletedByTransactionId(@Param("transactionId") String transactionId);

    @Query("SELECT COUNT(c) FROM CompensationLogEntity c WHERE c.transactionId = :transactionId AND c.status = 'FAILED'")
    long countFailedByTransactionId(@Param("transactionId") String transactionId);

    // Обновление статуса компенсации
    @Modifying
    @Transactional
    @Query("UPDATE CompensationLogEntity c SET c.status = :status, c.completedAt = :completedAt, c.durationMs = :durationMs WHERE c.id = :logId")
    int updateCompensationStatus(
            @Param("logId") Long logId,
            @Param("status") String status,
            @Param("completedAt") LocalDateTime completedAt,
            @Param("durationMs") Long durationMs);

    // Обновление статуса с ошибкой
    @Modifying
    @Transactional
    @Query("UPDATE CompensationLogEntity c SET c.status = 'FAILED', c.errorMessage = :errorMessage, c.retryCount = c.retryCount + 1 WHERE c.id = :logId")
    int markCompensationAsFailed(
            @Param("logId") Long logId,
            @Param("errorMessage") String errorMessage);

    // Поиск компенсаций по временному диапазону
    @Query("SELECT c FROM CompensationLogEntity c WHERE c.createdAt BETWEEN :startDate AND :endDate")
    List<CompensationLogEntity> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Получение последней компенсации для транзакции
    @Query("SELECT c FROM CompensationLogEntity c WHERE c.transactionId = :transactionId ORDER BY c.createdAt DESC LIMIT 1")
    Optional<CompensationLogEntity> findLatestByTransactionId(@Param("transactionId") String transactionId);

    // Проверка существования активной компенсации
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CompensationLogEntity c WHERE c.transactionId = :transactionId AND c.status IN ('PENDING', 'IN_PROGRESS')")
    boolean existsActiveCompensation(@Param("transactionId") String transactionId);

    // Поиск компенсаций для определенного товара
    @Query(value = "SELECT c FROM CompensationLogEntity c WHERE c.compensatedItems LIKE %:productId%")
    List<CompensationLogEntity> findByProductId(@Param("productId") String productId);

    // Группировка по типу компенсации
    @Query("SELECT c.compensationType, COUNT(c) FROM CompensationLogEntity c GROUP BY c.compensationType")
    List<Object[]> countByCompensationType();

    // Среднее время компенсации
    @Query("SELECT AVG(c.durationMs) FROM CompensationLogEntity c WHERE c.status = 'COMPLETED' AND c.durationMs IS NOT NULL")
    Optional<Double> findAverageCompensationDuration();
}