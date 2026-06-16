package com.solventa.solventa_backend.quotes.repository;

import com.solventa.solventa_backend.quotes.model.Quote;

import java.math.BigDecimal;

public interface QuoteStatusAggregation {
    Quote.QuoteStatus getStatus();
    Long getCount();
    BigDecimal getTotal();
}