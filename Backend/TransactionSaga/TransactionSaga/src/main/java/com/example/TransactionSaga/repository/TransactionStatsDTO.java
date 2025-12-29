package com.example.TransactionSaga.model.dto;

import java.time.LocalDateTime;

public class TransactionStatsDTO {
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private long totalTransactions;
    private long completedTransactions;
    private long cancelledTransactions;
    private long problemTransactions;
    private long pendingTransactions;
    private double avgCompletionTimeMinutes;
    private double successRate;
    private long totalItemsScanned;
    private long totalItemsMissing;

    // Геттеры и сеттеры
    public LocalDateTime getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDateTime periodStart) { this.periodStart = periodStart; }

    public LocalDateTime getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDateTime periodEnd) { this.periodEnd = periodEnd; }

    public long getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(long totalTransactions) { this.totalTransactions = totalTransactions; }

    public long getCompletedTransactions() { return completedTransactions; }
    public void setCompletedTransactions(long completedTransactions) { this.completedTransactions = completedTransactions; }

    public long getCancelledTransactions() { return cancelledTransactions; }
    public void setCancelledTransactions(long cancelledTransactions) { this.cancelledTransactions = cancelledTransactions; }

    public long getProblemTransactions() { return problemTransactions; }
    public void setProblemTransactions(long problemTransactions) { this.problemTransactions = problemTransactions; }

    public long getPendingTransactions() { return pendingTransactions; }
    public void setPendingTransactions(long pendingTransactions) { this.pendingTransactions = pendingTransactions; }

    public double getAvgCompletionTimeMinutes() { return avgCompletionTimeMinutes; }
    public void setAvgCompletionTimeMinutes(double avgCompletionTimeMinutes) { this.avgCompletionTimeMinutes = avgCompletionTimeMinutes; }

    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }

    public long getTotalItemsScanned() { return totalItemsScanned; }
    public void setTotalItemsScanned(long totalItemsScanned) { this.totalItemsScanned = totalItemsScanned; }

    public long getTotalItemsMissing() { return totalItemsMissing; }
    public void setTotalItemsMissing(long totalItemsMissing) { this.totalItemsMissing = totalItemsMissing; }

    // Вспомогательные методы
    public double getCancellationRate() {
        return totalTransactions > 0 ? (cancelledTransactions * 100.0) / totalTransactions : 0;
    }

    public double getProblemRate() {
        return totalTransactions > 0 ? (problemTransactions * 100.0) / totalTransactions : 0;
    }

    public long getSuccessfulTransactions() {
        return completedTransactions;
    }

    public double getEfficiencyScore() {
        if (totalTransactions == 0) return 0;
        double score = successRate * 0.6;
        if (avgCompletionTimeMinutes > 0) {
            score += (60.0 / Math.max(avgCompletionTimeMinutes, 1)) * 0.4;
        }
        return Math.min(score, 100);
    }
}