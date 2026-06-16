package com.solventa.solventa_backend.quotes.dto;

import com.solventa.solventa_backend.quotes.model.Quote;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuoteStatusRequest {

    @NotNull(message = "El estado es requerido")
    private Quote.QuoteStatus status;
}