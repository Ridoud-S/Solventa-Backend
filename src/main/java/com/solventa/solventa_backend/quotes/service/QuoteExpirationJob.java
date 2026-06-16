package com.solventa.solventa_backend.quotes.service;

import com.solventa.solventa_backend.quotes.model.Quote;
import com.solventa.solventa_backend.quotes.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuoteExpirationJob {

    private final QuoteRepository quoteRepository;

    /**
     * Corre cada hora. Marca como EXPIRED todas las cotizaciones SENT
     * cuya fecha de vencimiento ya pasó. Es global — no depende de TenantContext
     * porque corre fuera de cualquier request HTTP.
     */
    @Scheduled(cron = "0 0 * * * *") // cada hora en punto
    @Transactional
    public void expireOverdueQuotes() {
        var overdue = quoteRepository.findAllByStatusAndExpiresAtBeforeAndDeletedAtIsNull(
                Quote.QuoteStatus.SENT, LocalDate.now());

        if (overdue.isEmpty()) {
            return;
        }

        for (Quote quote : overdue) {
            quote.setStatus(Quote.QuoteStatus.EXPIRED);
            quoteRepository.save(quote);
        }

        log.info("Job de vencimiento: {} cotización(es) marcadas como EXPIRED", overdue.size());
    }
}