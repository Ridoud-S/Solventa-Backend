package com.solventa.solventa_backend.quotes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuoteLineRequest {

    @NotBlank(message = "La descripción de la línea es requerida")
    private String description;

    @NotNull(message = "La cantidad es requerida")
    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a 0")
    private BigDecimal quantity;

    @NotNull(message = "El precio unitario es requerido")
    @DecimalMin(value = "0.00", message = "El precio unitario no puede ser negativo")
    private BigDecimal unitPrice;
}