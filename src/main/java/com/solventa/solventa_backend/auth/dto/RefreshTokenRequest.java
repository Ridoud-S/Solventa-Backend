package com.solventa.solventa_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {

    @NotBlank(message = "El refresh token es requerido")
    private String refreshToken;
}