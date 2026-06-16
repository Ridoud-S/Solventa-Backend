package com.solventa.solventa_backend.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class DashboardStatsResponse {

    private long totalLeads;
    private long totalCustomers;
    private long openQuotesCount;
    private BigDecimal openQuotesValue;
    private long todayReminders;
    private Map<String, QuoteStatusStat> quotesByStatus;

    @Data
    @Builder
    public static class QuoteStatusStat {
        private long count;
        private BigDecimal total;
    }
}