package com.solventa.solventa_backend.quotes.dto;

import com.solventa.solventa_backend.quotes.model.Quote;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class QuoteResponse {

    private UUID id;
    private CustomerInfo customer;
    private String title;
    private Quote.QuoteStatus status;
    private List<QuoteLineResponse> lines;
    private BigDecimal discountPct;
    private BigDecimal taxPct;
    private BigDecimal subtotal;
    private BigDecimal total;
    private String notes;
    private LocalDate issuedAt;
    private LocalDate expiresAt;
    private UserInfo createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @Data
    @Builder
    public static class CustomerInfo {
        private UUID id;
        private String name;
        private String company;
    }

    @Data
    @Builder
    public static class UserInfo {
        private UUID id;
        private String name;
    }

    // ── Mapper estático ────────────────────────────────────────────────────────
    public static QuoteResponse from(Quote quote) {
        return QuoteResponse.builder()
                .id(quote.getId())
                .customer(CustomerInfo.builder()
                        .id(quote.getCustomer().getId())
                        .name(quote.getCustomer().getName())
                        .company(quote.getCustomer().getCompany())
                        .build())
                .title(quote.getTitle())
                .status(quote.getStatus())
                .lines(quote.getLines().stream()
                        .map(QuoteLineResponse::from)
                        .toList())
                .discountPct(quote.getDiscountPct())
                .taxPct(quote.getTaxPct())
                .subtotal(quote.getSubtotal())
                .total(quote.getTotal())
                .notes(quote.getNotes())
                .issuedAt(quote.getIssuedAt())
                .expiresAt(quote.getExpiresAt())
                .createdBy(quote.getCreatedBy() != null
                        ? UserInfo.builder()
                        .id(quote.getCreatedBy().getId())
                        .name(quote.getCreatedBy().getName())
                        .build()
                        : null)
                .createdAt(quote.getCreatedAt())
                .updatedAt(quote.getUpdatedAt())
                .build();
    }
}