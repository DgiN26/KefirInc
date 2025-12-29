package office.office;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/office")
public class OfficeController {

    @Autowired
    private OfficeService officeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ========== ИНФОРМАЦИОННЫЕ ЭНДПОИНТЫ ==========

    @GetMapping("/product/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Integer id) {
        try {
            Map<String, Object> product = officeService.getProductInfo(id);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/order/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Integer id) {
        try {
            Map<String, Object> order = officeService.getOrderInfo(id);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/order/{id}/client")
    public ResponseEntity<?> getClientByOrder(@PathVariable Integer id) {
        try {
            Map<String, Object> client = officeService.getUserInfoByOrder(id);
            return ResponseEntity.ok(client);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/info/{orderId}/{productId}")
    public ResponseEntity<?> getFullInfo(
            @PathVariable Integer orderId,
            @PathVariable Integer productId) {
        try {
            Map<String, Object> info = officeService.getFullInfo(orderId, productId);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========== ЭНДПОИНТЫ ДЛЯ ПРОБЛЕМ ==========

    @PostMapping("/problem")
    public ResponseEntity<?> createProblem(@RequestBody Map<String, Object> request) {
        try {
            Integer orderId = (Integer) request.get("orderId");
            Integer productId = (Integer) request.get("productId");
            String collectorId = (String) request.get("collectorId");
            String problemType = (String) request.get("problemType");
            String details = (String) request.get("details");

            OfficeProblem problem = officeService.createProblem(orderId, productId, collectorId,
                    problemType, details);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "problem", problem,
                    "message", "Проблема зарегистрирована"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/problems")
    public ResponseEntity<?> getAllProblems() {
        try {
            List<OfficeProblem> problems = officeService.getActiveProblems();
            return ResponseEntity.ok(problems);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/problem/{id}")
    public ResponseEntity<?> getProblem(@PathVariable Long id) {
        try {
            OfficeProblem problem = officeService.getProblemById(id);
            return ResponseEntity.ok(problem);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Проблема не найдена: " + e.getMessage()));
        }
    }

    @PostMapping("/problem/{id}/notify")
    public ResponseEntity<?> notifyClient(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            String message = (String) request.get("message");

            // ПРАВИЛЬНЫЙ ВЫЗОВ: problemId и message
            OfficeProblem problem = officeService.notifyClient(id, message);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "problem", problem,
                    "message", "Клиент уведомлен"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/problem/{id}/decision")
    public ResponseEntity<?> updateDecision(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            String decision = (String) request.get("decision");
            String comments = (String) request.get("comments");

            OfficeProblem problem = officeService.updateClientDecision(id, decision, comments);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "problem", problem,
                    "message", "Решение клиента сохранено"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/problem/{id}/resolve")
    public ResponseEntity<?> resolveProblem(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            String officeAction = (String) request.get("officeAction");
            String solution = (String) request.get("solution");

            OfficeProblem problem = officeService.resolveProblem(id, officeAction, solution);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "problem", problem,
                    "message", "Проблема решена"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/problems/collector/{collectorId}")
    public ResponseEntity<?> getProblemsByCollector(@PathVariable String collectorId) {
        try {
            List<OfficeProblem> problems = officeService.getProblemsByCollector(collectorId);
            return ResponseEntity.ok(problems);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========== ДАШБОРД ==========

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            Map<String, Object> stats = officeService.getDashboardStats();
            List<OfficeProblem> recentProblems = officeService.getRecentProblems(10);

            return ResponseEntity.ok(Map.of(
                    "stats", stats,
                    "recentProblems", recentProblems,
                    "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/dashboard/unprocessed")
    public ResponseEntity<?> getUnprocessedProblems() {
        try {
            List<OfficeProblem> problems = officeService.getUnprocessedProblems();
            return ResponseEntity.ok(problems);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========== ПРОВЕРКА ЗДОРОВЬЯ ==========

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            Map<String, Object> health = officeService.healthCheck();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "status", "DOWN",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            ));
        }
    }

    // ========== ТЕСТОВЫЕ ДАННЫЕ ==========

    @PostMapping("/test/problem")
    public ResponseEntity<?> createTestProblem() {
        try {
            Random random = new Random();
            Integer orderId = random.nextInt(100) + 1;
            Integer productId = random.nextInt(50) + 1;
            String collectorId = "COLLECTOR_TEST_" + (random.nextInt(10) + 1);

            OfficeProblem problem = officeService.createProblem(
                    orderId, productId, collectorId,
                    "MISSING_PRODUCT", "Тестовая проблема"
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "problem", problem,
                    "message", "Тестовая проблема создана"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/test/simulate")
    public ResponseEntity<?> simulateProblem() {
        try {
            Random random = new Random();
            String collectorId = "COLLECTOR_" + (random.nextInt(10) + 1);

            Map<String, Object> result = officeService.simulateCollectorProblem(collectorId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}