package com.solventa.solventa_backend.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCompanyRequest {

    @NotBlank(message = "El nombre de la empresa es requerido")
    @Size(min = 2, max = 200)
    private String name;

    @Size(max = 100)
    private String industry;

    @Size(max = 13)
    private String rfc;
}