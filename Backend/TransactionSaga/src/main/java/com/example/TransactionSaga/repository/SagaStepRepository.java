package com.example.TransactionSaga.repository;

import com.example.TransactionSaga.model.entity.SagaStepEntity;
import com.example.TransactionSaga.model.enums.SagaStepType;
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
public interface SagaStepRepository extends JpaRepository<SagaStepEntity, Long> {

    // Поиск по ID транзакции
    List<SagaStepEntity> findByTransactionId(String transactionId);

    List<SagaStepEntity> findByTransactionIdOrderByStepOrderAsc(String transactionId);

    // Поиск по статусу
    List<SagaStepEntity> findByTransactionIdAndStatus(String transactionId, String status);

    List<SagaStepEntity> findByStatus(String status);

    // Поиск по типу шага
    Optional<SagaStepEntity> findByTransactionIdAndStepType(String transactionId, SagaStepType stepType);

    List<SagaStepEntity> findByStepType(SagaStepType stepType);

    // Поиск шагов для повторной попытки
    @Query("SELECT s FROM SagaStepEntity s WHERE s.status = 'FAILED' AND s.retryCount < s.maxRetries AND (s.nextRetryTime IS NULL OR s.nextRetryTime <= CURRENT_TIMESTAMP)")
    List<SagaStepEntity> findStepsForRetry();

    // Поиск незавершенных шагов
    @Query("SELECT s FROM SagaStepEntity s WHERE s.transactionId = :transactionId AND s.status IN ('PENDING', 'IN_PROGRESS', 'FAILED')")
    List<SagaStepEntity> findIncompleteStepsByTransactionId(@Param("transactionId") String transactionId);

    // Поиск компенсируемых шагов
    @Query("SELECT s FROM SagaStepEntity s WHERE s.transactionId = :transactionId AND s.isCompensatable = true AND s.status = 'COMPLETED'")
    List<SagaStepEntity> findCompensatableStepsByTransactionId(@Param("transactionId") String transactionId);

    // Подсчет шагов
    long countByTransactionId(String transactionId);

    long countByTransactionIdAndStatus(String transactionId, String status);

    // Обновление статуса шага
    @Modifying
    @Transactional
    @Query("UPDATE SagaStepEntity s SET s.status = :status, s.completedAt = :completedAt, s.durationMs = :durationMs WHERE s.id = :stepId")
    int updateStepStatus(
            @Param("stepId") Long stepId,
            @Param("status") String status,
            @Param("completedAt") LocalDateTime completedAt,
            @Param("durationMs") Long durationMs);

    // Обновление статуса с ошибкой
    @Modifying
    @Transactional
    @Query("UPDATE SagaStepEntity s SET s.status = 'FAILED', s.errorMessage = :errorMessage, s.retryCount = s.retryCount + 1, s.nextRetryTime = :nextRetryTime WHERE s.id = :stepId")
    int markStepAsFailed(
            @Param("stepId") Long stepId,
            @Param("errorMessage") String errorMessage,
            @Param("nextRetryTime") LocalDateTime nextRetryTime);

    // Пропуск шага
    @Modifying
    @Transactional
    @Query("UPDATE SagaStepEntity s SET s.status = 'SKIPPED', s.completedAt = CURRENT_TIMESTAMP WHERE s.id = :stepId")
    int skipStep(@Param("stepId") Long stepId);

    // Отметка как компенсированного
    @Modifying
    @Transactional
    @Query("UPDATE SagaStepEntity s SET s.status = 'COMPENSATED', s.compensatedAt = CURRENT_TIMESTAMP WHERE s.id = :stepId")
    int markAsCompensated(@Param("stepId") Long stepId);

    // Поиск шагов по зависимости
    @Query("SELECT s FROM SagaStepEntity s WHERE s.dependsOnStepId = :stepId")
    List<SagaStepEntity> findByDependsOnStepId(@Param("stepId") Long stepId);

    // Получение следующего шага по порядку
    @Query("SELECT s FROM SagaStepEntity s WHERE s.transactionId = :transactionId AND s.stepOrder > :currentOrder ORDER BY s.stepOrder ASC LIMIT 1")
    Optional<SagaStepEntity> findNextStep(
            @Param("transactionId") String transactionId,
            @Param("currentOrder") Integer currentOrder);

    // Проверка готовности шага к выполнению
    @Query("SELECT CASE WHEN COUNT(s) = 0 THEN true ELSE false END FROM SagaStepEntity s WHERE s.transactionId = :transactionId AND s.dependsOnStepId = :stepId AND s.status != 'COMPLETED'")
    boolean isStepReadyToExecute(
            @Param("transactionId") String transactionId,
            @Param("stepId") Long stepId);

    // Поиск шагов для компенсации (обратный порядок)
    @Query("SELECT s FROM SagaStepEntity s WHERE s.transactionId = :transactionId AND s.isCompensatable = true AND s.status = 'COMPLETED' ORDER BY s.stepOrder DESC")
    List<SagaStepEntity> findStepsForCompensation(@Param("transactionId") String transactionId);
}