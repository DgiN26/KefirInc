package com.Auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

@Service
public class TokenBlacklistService {

    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Transactional
    public void blacklistToken(String token) {
        try {
            String username = jwtUtil.extractUsername(token);
            Date expirationDate = jwtUtil.extractExpiration(token);
            LocalDateTime expiresAt = expirationDate.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();

            if (!blacklistedTokenRepository.existsByToken(token)) {
                BlacklistedToken blacklistedToken = new BlacklistedToken(token, username, expiresAt);
                blacklistedTokenRepository.save(blacklistedToken);
                System.out.println("Token blacklisted for user: " + username);
            }
        } catch (Exception e) {
            System.err.println("Error blacklisting token: " + e.getMessage());
            throw new RuntimeException("Failed to blacklist token");
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokenRepository.existsByToken(token);
    }

    @Transactional
    public void removeFromBlacklist(String token) {
        try {
            blacklistedTokenRepository.deleteByToken(token);
            System.out.println("Removed token from blacklist");
        } catch (Exception e) {
            System.err.println("Error removing token from blacklist: " + e.getMessage());
        }
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            System.out.println("Starting cleanup of expired blacklisted tokens...");

            long beforeCount = blacklistedTokenRepository.count();
            System.out.println("Tokens before cleanup: " + beforeCount);

            if (beforeCount > 0) {
                blacklistedTokenRepository.deleteExpiredTokens(now);
                long afterCount = blacklistedTokenRepository.count();
                long removedCount = beforeCount - afterCount;

                if (removedCount > 0) {
                    System.out.println("Cleaned up " + removedCount + " expired blacklisted tokens");
                } else {
                    System.out.println("No expired tokens found to clean up");
                }
            } else {
                System.out.println("No blacklisted tokens to clean up");
            }

            System.out.println("Cleanup of expired blacklisted tokens completed");
        } catch (Exception e) {
            System.err.println("Error cleaning up expired tokens: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public long getBlacklistedTokensCount() {
        return blacklistedTokenRepository.count();
    }

    public long getBlacklistedTokensCountForUser(String username) {
        return blacklistedTokenRepository.countByUsername(username);
    }
}