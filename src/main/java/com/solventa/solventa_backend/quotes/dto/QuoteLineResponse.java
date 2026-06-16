package com.solventa.solventa_backend.quotes.dto;

import com.solventa.solventa_backend.quotes.model.QuoteLine;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class QuoteLineResponse {

    private UUID id;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private Integer sortOrder;

    public static QuoteLineResponse from(QuoteLine line) {
        return QuoteLineResponse.builder()
                .id(line.getId())
                .description(line.getDescription())
                .quantity(line.getQuantity())
                .unitPrice(line.getUnitPrice())
                .subtotal(line.getSubtotal())
                .sortOrder(line.getSortOrder())
                .build();
    }
}