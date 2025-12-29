package com.Auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SessionService {

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public UserSession createUserSession(String sessionUUID, Integer userId, String jwtToken) {
        System.out.println("Creating user session: UUID=" + sessionUUID + ", userId=" + userId);

        userSessionRepository.deactivateUserSessions(userId);

        UserSession session = new UserSession();
        session.setSessionUUID(sessionUUID);
        session.setUserId(userId);
        session.setJwtToken(jwtToken);
        session.setClientToken("auth-" + sessionUUID);
        session.setIsActive(true);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusDays(1));

        UserSession savedSession = userSessionRepository.save(session);
        System.out.println("User session created with ID: " + savedSession.getId());

        return savedSession;
    }

    @Transactional(readOnly = true)
    public Optional<UserSession> validateSession(String clientToken) {
        if (clientToken == null || !clientToken.startsWith("auth-")) {
            return Optional.empty();
        }

        String sessionUUID = clientToken.substring(5);
        return userSessionRepository.findBySessionUUIDAndIsActive(sessionUUID, true)
                .filter(session -> session.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserBySession(String clientToken) {
        return validateSession(clientToken)
                .flatMap(session -> userRepository.findById(session.getUserId()));
    }

    @Transactional
    public void invalidateSession(String sessionUUID) {
        userSessionRepository.findBySessionUUID(sessionUUID)
                .ifPresent(session -> {
                    session.setIsActive(false);
                    userSessionRepository.save(session);
                    System.out.println("Session invalidated: " + sessionUUID);
                });
    }

    @Transactional
    public void invalidateAllUserSessions(Integer userId) {
        userSessionRepository.deactivateUserSessions(userId);
        System.out.println("All sessions invalidated for user ID: " + userId);
    }

    @Transactional(readOnly = true)
    public Optional<String> getJwtTokenBySession(String sessionUUID) {
        return userSessionRepository.findBySessionUUID(sessionUUID)
                .map(UserSession::getJwtToken);
    }

    @Transactional(readOnly = true)
    public Optional<String> getSessionUuidByJwt(String jwtToken) {
        return userSessionRepository.findAll().stream()
                .filter(session -> jwtToken.equals(session.getJwtToken()) && Boolean.TRUE.equals(session.getIsActive()))
                .findFirst()
                .map(UserSession::getSessionUUID);
    }

    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        userSessionRepository.deleteExpiredSessions(now);
        System.out.println("Expired sessions cleanup completed at: " + now);
    }

    @Transactional(readOnly = true)
    public long getActiveSessionsCount() {
        return userSessionRepository.findAll().stream()
                .filter(session -> Boolean.TRUE.equals(session.getIsActive()))
                .filter(session -> session.getExpiresAt().isAfter(LocalDateTime.now()))
                .count();
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void scheduledSessionCleanup() {
        System.out.println("=== STARTING SCHEDULED SESSION CLEANUP ===");
        try {
            cleanupExpiredSessions();
            System.out.println("Session cleanup completed successfully");
        } catch (Exception e) {
            System.err.println("Error during scheduled session cleanup: " + e.getMessage());
        }
        System.out.println("=== SCHEDULED SESSION CLEANUP COMPLETED ===");
    }
}