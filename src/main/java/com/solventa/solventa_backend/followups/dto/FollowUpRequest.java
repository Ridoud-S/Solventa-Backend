package com.solventa.solventa_backend.followups.dto;

import com.solventa.solventa_backend.followups.model.FollowUp;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class FollowUpRequest {

    @NotNull(message = "El tipo de entidad es requerido (LEAD o CUSTOMER)")
    private FollowUp.EntityType entityType;

    @NotNull(message = "El ID de la entidad es requerido")
    private UUID entityId;

    @NotNull(message = "El tipo de seguimiento es requerido")
    private FollowUp.FollowUpType type;

    @NotNull(message = "La fecha de interacción es requerida")
    private OffsetDateTime interactionDate;

    @NotBlank(message = "Las notas son requeridas")
    private String notes;

    private String result;
}