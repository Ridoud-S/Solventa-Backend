package com.solventa.solventa_backend.users.dto;

import com.solventa.solventa_backend.users.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InviteUserRequest {

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 150)
    private String name;

    @NotBlank(message = "El correo es requerido")
    @Email(message = "Correo inválido")
    private String email;

    @NotNull(message = "El rol es requerido")
    private Role role;
}