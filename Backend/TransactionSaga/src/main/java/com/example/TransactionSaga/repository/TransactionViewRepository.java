package com.example.TransactionSaga.repository;

import com.example.TransactionSaga.model.dto.TransactionStatsDTO;
import com.example.TransactionSaga.model.entity.TransactionEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface TransactionViewRepository extends Repository<TransactionEntity, String> {

    // Статистика по статусам
    @Query(value = """
        SELECT 
            status,
            COUNT(*) as count,
            ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM transactions), 2) as percentage
        FROM transactions
        GROUP BY status
        ORDER BY count DESC
        """, nativeQuery = true)
    List<Object[]> getStatusStatistics();

    // Статистика по сборщикам
    @Query(value = """
        SELECT 
            collector_id,
            COUNT(*) as total_transactions,
            COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed,
            COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) as cancelled,
            COUNT(CASE WHEN status IN ('PAUSED', 'WAITING_CLIENT', 'WAITING_OFFICE') THEN 1 END) as problem_transactions,
            AVG(EXTRACT(EPOCH FROM (completed_at - started_at))/60) as avg_completion_minutes
        FROM transactions
        WHERE collector_id IS NOT NULL
        GROUP BY collector_id
        ORDER BY total_transactions DESC
        """, nativeQuery = true)
    List<Object[]> getCollectorStatistics();

    // Среднее время выполнения по статусам
    @Query(value = """
        SELECT 
            status,
            AVG(EXTRACT(EPOCH FROM (updated_at - created_at))/60) as avg_duration_minutes,
            MIN(EXTRACT(EPOCH FROM (updated_at - created_at))/60) as min_duration_minutes,
            MAX(EXTRACT(EPOCH FROM (updated_at - created_at))/60) as max_duration_minutes
        FROM transactions
        WHERE status IN ('COMPLETED', 'CANCELLED')
        GROUP BY status
        """, nativeQuery = true)
    List<Object[]> getAverageDurationByStatus();

    // Количество проблемных товаров по дням
    @Query(value = """
        SELECT 
            DATE(created_at) as date,
            SUM(jsonb_array_length(missing_items)) as missing_items_count,
            COUNT(DISTINCT id) as transactions_with_problems
        FROM transactions
        WHERE jsonb_array_length(missing_items) > 0
        GROUP BY DATE(created_at)
        ORDER BY date DESC
        LIMIT 30
        """, nativeQuery = true)
    List<Object[]> getDailyProblemStatistics();

    // Топ проблемных товаров
    @Query(value = """
        WITH product_counts AS (
            SELECT 
                key as product_id,
                SUM(value::int) as total_missing
            FROM transactions,
            jsonb_each_text(missing_items)
            GROUP BY key
        )
        SELECT 
            product_id,
            total_missing
        FROM product_counts
        ORDER BY total_missing DESC
        LIMIT 20
        """, nativeQuery = true)
    List<Object[]> getTopProblematicProducts();

    // Эффективность сборщиков
    @Query(value = """
        SELECT 
            collector_id,
            COUNT(*) as total_tasks,
            COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_tasks,
            ROUND(COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) * 100.0 / COUNT(*), 2) as success_rate,
            AVG(EXTRACT(EPOCH FROM (completed_at - started_at))/60) as avg_task_time_minutes,
            SUM(jsonb_array_length(scanned_items)) as total_items_scanned
        FROM transactions
        WHERE collector_id IS NOT NULL
        GROUP BY collector_id
        HAVING COUNT(*) >= 5
        ORDER BY success_rate DESC
        """, nativeQuery = true)
    List<Object[]> getCollectorEfficiency();

    // Время реакции клиентов
    @Query(value = """
        SELECT 
            EXTRACT(HOUR FROM (client_responded_at - client_notified_at)) as response_hours,
            COUNT(*) as response_count
        FROM transactions
        WHERE client_responded_at IS NOT NULL 
          AND client_notified_at IS NOT NULL
          AND status IN ('COMPLETED', 'CANCELLED')
        GROUP BY EXTRACT(HOUR FROM (client_responded_at - client_notified_at))
        ORDER BY response_hours
        """, nativeQuery = true)
    List<Object[]> getClientResponseTimeDistribution();

    // Процент отмененных заказов по причине
    @Query(value = """
        SELECT 
            CASE 
                WHEN pause_reason LIKE '%not available%' THEN 'Товар отсутствует'
                WHEN pause_reason LIKE '%damaged%' THEN 'Товар поврежден'
                WHEN pause_reason LIKE '%wrong%' THEN 'Неверный товар'
                ELSE 'Другая причина'
            END as reason_category,
            COUNT(*) as cancellation_count,
            ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM transactions WHERE status = 'CANCELLED'), 2) as percentage
        FROM transactions
        WHERE status = 'CANCELLED' AND pause_reason IS NOT NULL
        GROUP BY reason_category
        ORDER BY cancellation_count DESC
        """, nativeQuery = true)
    List<Object[]> getCancellationReasons();
}