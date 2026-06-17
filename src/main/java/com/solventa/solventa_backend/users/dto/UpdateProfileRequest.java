package com.solventa.solventa_backend.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 150)
    private String name;
}