package office.office;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OfficeProblemRepository extends JpaRepository<OfficeProblem, Long> {

    // ==================== БАЗОВЫЕ МЕТОДЫ ====================

    // Найти по ID проблемы
    @Override
    Optional<OfficeProblem> findById(Long id);

    // Найти все проблемы по статусу
    List<OfficeProblem> findByStatus(String status);

    // Найти проблемы по ID заказа
    List<OfficeProblem> findByOrderId(Integer orderId);

    // Найти проблемы по ID клиента
    List<OfficeProblem> findByClientId(Integer clientId);

    // Найти проблемы по ID сборщика
    List<OfficeProblem> findByCollectorId(String collectorId);

    // Найти проблемы по ID товара
    List<OfficeProblem> findByProductId(Integer productId);

    // Найти проблемы по типу проблемы
    List<OfficeProblem> findByProblemType(String problemType);

    // Найти последние N проблем
    List<OfficeProblem> findTop10ByOrderByCreatedAtDesc();

    // ==================== ПРОСТЫЕ ЗАПРОСЫ ====================

    // Найти активные проблемы (не решенные)
    @Query("SELECT p FROM OfficeProblem p WHERE p.status NOT IN ('RESOLVED', 'CANCELLED', 'COMPLETED') " +
            "ORDER BY p.createdAt DESC")
    List<OfficeProblem> findActiveProblems();

    // Найти проблемы, созданные сегодня
    @Query("SELECT p FROM OfficeProblem p WHERE DATE(p.createdAt) = CURRENT_DATE")
    List<OfficeProblem> findTodayProblems();

    // Посчитать количество проблем по статусу
    @Query("SELECT COUNT(p) FROM OfficeProblem p WHERE p.status = :status")
    Long countByStatus(@Param("status") String status);

    // Посчитать количество проблем за сегодня по статусу
    @Query("SELECT COUNT(p) FROM OfficeProblem p WHERE p.status = :status AND DATE(p.createdAt) = CURRENT_DATE")
    Long countTodayByStatus(@Param("status") String status);

    // ==================== ДЛЯ ДАШБОРДА ====================

    // Найти проблемы для дашборда (сортировка по приоритету)
    @Query("SELECT p FROM OfficeProblem p WHERE p.status IN ('PENDING', 'CLIENT_NOTIFIED') " +
            "ORDER BY " +
            "CASE p.priority " +
            "  WHEN 'HIGH' THEN 1 " +
            "  WHEN 'MEDIUM' THEN 2 " +
            "  WHEN 'LOW' THEN 3 " +
            "  ELSE 4 " +
            "END ASC, " +
            "p.createdAt ASC")
    List<OfficeProblem> findProblemsForDashboard();

    // Найти необработанные проблемы (старые более 1 часа)
    @Query("SELECT p FROM OfficeProblem p WHERE p.status = 'PENDING' AND p.createdAt < :threshold")
    List<OfficeProblem> findUnprocessedProblems(@Param("threshold") LocalDateTime threshold);

    // ==================== ПРОВЕРОЧНЫЕ ЗАПРОСЫ ====================

    // Проверить существование активной проблемы для заказа и товара
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM OfficeProblem p " +
            "WHERE p.orderId = :orderId AND p.productId = :productId " +
            "AND p.status NOT IN ('RESOLVED', 'CANCELLED')")
    boolean existsActiveProblemForOrderAndProduct(@Param("orderId") Integer orderId,
                                                  @Param("productId") Integer productId);
}