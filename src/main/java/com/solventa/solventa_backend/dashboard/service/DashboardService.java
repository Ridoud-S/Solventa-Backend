package com.solventa.solventa_backend.dashboard.service;

import com.solventa.solventa_backend.customers.repository.CustomerRepository;
import com.solventa.solventa_backend.dashboard.dto.DashboardStatsResponse;
import com.solventa.solventa_backend.followups.repository.ReminderRepository;
import com.solventa.solventa_backend.leads.repository.LeadRepository;
import com.solventa.solventa_backend.quotes.model.Quote;
import com.solventa.solventa_backend.quotes.repository.QuoteRepository;
import com.solventa.solventa_backend.shared.context.TenantContext;
import com.solventa.solventa_backend.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final LeadRepository     leadRepository;
    private final CustomerRepository customerRepository;
    private final QuoteRepository    quoteRepository;
    private final ReminderRepository reminderRepository;

    // ── Estados que cuentan como "pipeline abierto" ────────────────────────────
    private static final List<Quote.QuoteStatus> OPEN_STATUSES =
            List.of(Quote.QuoteStatus.DRAFT, Quote.QuoteStatus.SENT);

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId   = SecurityUtils.getCurrentUserId();

        long totalLeads     = leadRepository.countByTenantIdAndDeletedAtIsNull(tenantId);
        long totalCustomers = customerRepository.countByTenantIdAndDeletedAtIsNull(tenantId);

        // ── Inicializar los 5 estados en cero — el frontend espera todas las keys ──
        Map<String, DashboardStatsResponse.QuoteStatusStat> quotesByStatus = new LinkedHashMap<>();
        for (Quote.QuoteStatus status : Quote.QuoteStatus.values()) {
            quotesByStatus.put(status.name(), DashboardStatsResponse.QuoteStatusStat.builder()
                    .count(0)
                    .total(BigDecimal.ZERO)
                    .build());
        }

        long openQuotesCount = 0;
        BigDecimal openQuotesValue = BigDecimal.ZERO;

        for (var agg : quoteRepository.findStatusAggregations(tenantId)) {
            quotesByStatus.put(agg.getStatus().name(), DashboardStatsResponse.QuoteStatusStat.builder()
                    .count(agg.getCount())
                    .total(agg.getTotal())
                    .build());

            if (OPEN_STATUSES.contains(agg.getStatus())) {
                openQuotesCount += agg.getCount();
                openQuotesValue = openQuotesValue.add(agg.getTotal());
            }
        }

        // ── Recordatorios pendientes de hoy para el usuario en sesión ──────────────
        var range = todayRange();
        long todayReminders = reminderRepository.countTodayForUser(
                tenantId, userId, range[0], range[1]);

        return DashboardStatsResponse.builder()
                .totalLeads(totalLeads)
                .totalCustomers(totalCustomers)
                .openQuotesCount(openQuotesCount)
                .openQuotesValue(openQuotesValue)
                .todayReminders(todayReminders)
                .quotesByStatus(quotesByStatus)
                .build();
    }

    private OffsetDateTime[] todayRange() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime start = now.toLocalDate().atStartOfDay(now.getOffset()).toOffsetDateTime();
        OffsetDateTime end   = start.plusDays(1);
        return new OffsetDateTime[]{start, end};
    }
}