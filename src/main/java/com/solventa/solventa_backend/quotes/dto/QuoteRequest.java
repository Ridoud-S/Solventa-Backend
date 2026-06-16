package com.solventa.solventa_backend.quotes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class QuoteRequest {

    @NotNull(message = "El cliente es requerido")
    private UUID customerId;

    @NotBlank(message = "El título es requerido")
    @Size(max = 300)
    private String title;

    @DecimalMin(value = "0.00", message = "El descuento no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El descuento no puede ser mayor a 100%")
    private BigDecimal discountPct = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "El IVA no puede ser negativo")
    private BigDecimal taxPct = new BigDecimal("16.00");

    private LocalDate issuedAt;

    @NotNull(message = "La fecha de vencimiento es requerida")
    @Future(message = "La fecha de vencimiento debe ser futura")
    private LocalDate expiresAt;

    private String notes;

    @NotEmpty(message = "La cotización debe tener al menos una línea")
    @Valid
    private List<QuoteLineRequest> lines;
}