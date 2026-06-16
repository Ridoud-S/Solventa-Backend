package com.solventa.solventa_backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "El correo es requerido")
    @Email(message = "Correo inválido")
    private String email;

    @NotBlank(message = "La contraseña es requerida")
    private String password;
}