package com.solventa.solventa_backend.leads.dto;

import com.solventa.solventa_backend.leads.model.Lead;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LeadRequest {

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 150)
    private String name;

    @Size(max = 200)
    private String company;

    @Email(message = "Correo inválido")
    @Size(max = 200)
    private String email;

    @Size(max = 30)
    private String phone;

    @NotNull(message = "La fuente es requerida")
    private Lead.LeadSource source;

    @NotNull(message = "La prioridad es requerida")
    private Lead.LeadPriority priority;

    private String notes;

    private String assignedToId;
}