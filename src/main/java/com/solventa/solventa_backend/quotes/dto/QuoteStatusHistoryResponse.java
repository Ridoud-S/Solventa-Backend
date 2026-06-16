package com.solventa.solventa_backend.quotes.dto;

import com.solventa.solventa_backend.quotes.model.Quote;
import com.solventa.solventa_backend.quotes.model.QuoteStatusHistory;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class QuoteStatusHistoryResponse {

    private Quote.QuoteStatus oldStatus;
    private Quote.QuoteStatus newStatus;
    private String changedByName;
    private OffsetDateTime changedAt;

    public static QuoteStatusHistoryResponse from(QuoteStatusHistory h) {
        return QuoteStatusHistoryResponse.builder()
                .oldStatus(h.getOldStatus())
                .newStatus(h.getNewStatus())
                .changedByName(h.getChangedBy() != null ? h.getChangedBy().getName() : "Sistema")
                .changedAt(h.getChangedAt())
                .build();
    }
}