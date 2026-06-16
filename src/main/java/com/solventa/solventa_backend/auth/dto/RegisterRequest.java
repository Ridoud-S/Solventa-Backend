package com.solventa.solventa_backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "El nombre de la empresa es requerido")
    @Size(min = 2, max = 200)
    private String companyName;

    @NotBlank(message = "Tu nombre es requerido")
    @Size(min = 2, max = 150)
    private String name;

    @NotBlank(message = "El correo es requerido")
    @Email(message = "Correo inválido")
    private String email;

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;
}