package com.Auth;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "blacklisted_tokens")
public class BlacklistedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "blacklisted_at", updatable = false)
    private LocalDateTime blacklistedAt;

    @Column(name = "reason", length = 100)
    private String reason;

    public BlacklistedToken() {
    }

    public BlacklistedToken(String token, String username, LocalDateTime expiresAt) {
        this.token = token;
        this.username = username;
        this.expiresAt = expiresAt;
        this.reason = "logout";
    }

    public BlacklistedToken(String token, String username, LocalDateTime expiresAt, String reason) {
        this.token = token;
        this.username = username;
        this.expiresAt = expiresAt;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getBlacklistedAt() { return blacklistedAt; }
    public void setBlacklistedAt(LocalDateTime blacklistedAt) { this.blacklistedAt = blacklistedAt; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}