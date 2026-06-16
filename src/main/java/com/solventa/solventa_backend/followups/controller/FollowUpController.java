package com.solventa.solventa_backend.followups.controller;

import com.solventa.solventa_backend.followups.dto.FollowUpRequest;
import com.solventa.solventa_backend.followups.dto.FollowUpResponse;
import com.solventa.solventa_backend.followups.model.FollowUp;
import com.solventa.solventa_backend.followups.service.FollowUpService;
import com.solventa.solventa_backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/follow-ups")
@RequiredArgsConstructor
@Tag(name = "FollowUps", description = "Historial de interacciones con leads y clientes")
public class FollowUpController {

    private final FollowUpService followUpService;

    @GetMapping
    @Operation(
            summary = "Listar seguimientos de una entidad",
            description = "Retorna el historial de interacciones (llamadas, emails, reuniones, WhatsApp) " +
                    "de un Lead o Customer, ordenado del más reciente al más antiguo."
    )
    public ResponseEntity<ApiResponse<List<FollowUpResponse>>> getByEntity(
            @RequestParam FollowUp.EntityType entityType,
            @RequestParam UUID entityId) {

        return ResponseEntity.ok(
                ApiResponse.ok(followUpService.findByEntity(entityType, entityId)));
    }

    @PostMapping
    @Operation(
            summary = "Registrar seguimiento",
            description = "Crea un registro de interacción con un Lead o Customer. " +
                    "El usuario que registra el seguimiento se toma del JWT en sesión."
    )
    public ResponseEntity<ApiResponse<FollowUpResponse>> create(
            @Valid @RequestBody FollowUpRequest req) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Seguimiento registrado", followUpService.create(req)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar seguimiento")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        followUpService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Seguimiento eliminado", null));
    }
}