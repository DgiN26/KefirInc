package com.Auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findBySessionUUID(String sessionUUID);

    Optional<UserSession> findBySessionUUIDAndIsActive(String sessionUUID, Boolean isActive);

    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId AND s.isActive = true")
    List<UserSession> findActiveSessionsByUserId(@Param("userId") Integer userId);

    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId AND s.isActive = true ORDER BY s.createdAt DESC")
    List<UserSession> findLatestActiveSessionByUserId(@Param("userId") Integer userId);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.userId = :userId")
    void deactivateUserSessions(@Param("userId") Integer userId);

    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM UserSession s WHERE s.jwtToken = :jwtToken AND s.isActive = true")
    Optional<UserSession> findByJwtToken(@Param("jwtToken") String jwtToken);

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.isActive = true AND s.expiresAt > :now")
    long countActiveSessions(@Param("now") LocalDateTime now);
}