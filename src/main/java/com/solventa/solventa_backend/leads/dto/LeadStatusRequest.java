package com.solventa.solventa_backend.leads.dto;

import com.solventa.solventa_backend.leads.model.Lead;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeadStatusRequest {

    @NotNull(message = "El estado es requerido")
    private Lead.LeadStatus status;
}