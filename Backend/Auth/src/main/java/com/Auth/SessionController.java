package com.Auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(origins = "*")
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/validate/{clientToken}")
    public ResponseEntity<?> validateSession(@PathVariable String clientToken) {
        try {
            Optional<UserSession> sessionOpt = sessionService.validateSession(clientToken);

            if (sessionOpt.isPresent()) {
                UserSession session = sessionOpt.get();
                Optional<User> userOpt = userRepository.findById(session.getUserId());

                if (userOpt.isPresent()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("valid", true);
                    response.put("sessionId", session.getSessionUUID());
                    response.put("userId", session.getUserId());
                    response.put("user", createUserResponse(userOpt.get()));
                    response.put("createdAt", session.getCreatedAt());
                    response.put("expiresAt", session.getExpiresAt());
                    response.put("isActive", session.getIsActive());
                    return ResponseEntity.ok(response);
                }
            }

            return ResponseEntity.status(401)
                    .body(Map.of(
                            "valid", false,
                            "message", "Session is invalid or expired"
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to validate session"));
        }
    }

    @GetMapping("/jwt/{clientToken}")
    public ResponseEntity<?> getJwtBySession(@PathVariable String clientToken) {
        try {
            if (!clientToken.startsWith("auth-")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid client token format"));
            }

            String sessionUUID = clientToken.substring(5);
            Optional<String> jwtOpt = sessionService.getJwtTokenBySession(sessionUUID);

            if (jwtOpt.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "jwtToken", jwtOpt.get(),
                        "sessionUUID", sessionUUID
                ));
            } else {
                return ResponseEntity.status(404)
                        .body(Map.of(
                                "success", false,
                                "error", "JWT token not found for session"
                        ));
            }

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get JWT token"));
        }
    }

    @GetMapping("/session/{jwtToken}")
    public ResponseEntity<?> getSessionByJwt(@PathVariable String jwtToken) {
        try {
            Optional<String> sessionUuidOpt = sessionService.getSessionUuidByJwt(jwtToken);

            if (sessionUuidOpt.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "sessionUUID", sessionUuidOpt.get(),
                        "clientToken", "auth-" + sessionUuidOpt.get()
                ));
            } else {
                return ResponseEntity.status(404)
                        .body(Map.of(
                                "success", false,
                                "error", "Session not found for JWT token"
                        ));
            }

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get session"));
        }
    }

    @PostMapping("/invalidate/{clientToken}")
    public ResponseEntity<?> invalidateSession(@PathVariable String clientToken) {
        try {
            if (!clientToken.startsWith("auth-")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid client token format"));
            }

            String sessionUUID = clientToken.substring(5);
            sessionService.invalidateSession(sessionUUID);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Session invalidated successfully",
                    "sessionUUID", sessionUUID
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to invalidate session"));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getSessionStats() {
        try {
            long activeSessions = sessionService.getActiveSessionsCount();

            Map<String, Object> stats = new HashMap<>();
            stats.put("activeSessions", activeSessions);
            stats.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "stats", stats
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get session stats"));
        }
    }

    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("id", user.getId());
        userResponse.put("username", user.getUsername());
        userResponse.put("firstname", user.getFirstname() != null ? user.getFirstname() : "");
        userResponse.put("email", user.getEmail() != null ? user.getEmail() : "");
        userResponse.put("role", user.getRole() != null ? user.getRole() : "client");
        userResponse.put("status", user.getStatus() != null ? user.getStatus() : "active");
        return userResponse;
    }
}