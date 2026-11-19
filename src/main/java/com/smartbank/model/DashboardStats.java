package com.smartbank.model;

import java.math.BigDecimal;

/** Aggregated metrics used by both customer and admin dashboards. */
public record DashboardStats(
        long customerCount,
        long accountCount,
        long activeAccountCount,
        BigDecimal totalBalance,
        BigDecimal monthlyDeposits,
        BigDecimal monthlyWithdrawals,
        long transactionCount,
        long openFraudAlerts
) {
    public static DashboardStats empty() {
        return new DashboardStats(0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0);
    }
}
