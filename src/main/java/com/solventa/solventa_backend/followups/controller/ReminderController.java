package com.solventa.solventa_backend.followups.controller;

import com.solventa.solventa_backend.followups.dto.ReminderRequest;
import com.solventa.solventa_backend.followups.dto.ReminderResponse;
import com.solventa.solventa_backend.followups.model.FollowUp;
import com.solventa.solventa_backend.followups.service.ReminderService;
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
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
@Tag(name = "Reminders", description = "Recordatorios de seguimiento")
public class ReminderController {

    private final ReminderService reminderService;

    @GetMapping
    @Operation(
            summary = "Listar recordatorios de una entidad",
            description = "Retorna los recordatorios asociados a un Lead o Customer, ordenados por fecha ascendente."
    )
    public ResponseEntity<ApiResponse<List<ReminderResponse>>> getByEntity(
            @RequestParam FollowUp.EntityType entityType,
            @RequestParam UUID entityId) {

        return ResponseEntity.ok(
                ApiResponse.ok(reminderService.findByEntity(entityType, entityId)));
    }

    @GetMapping("/today")
    @Operation(
            summary = "Recordatorios pendientes de hoy",
            description = "Retorna los recordatorios no completados del usuario en sesión cuya fecha " +
                    "cae dentro del día actual. Usado por el Dashboard."
    )
    public ResponseEntity<ApiResponse<List<ReminderResponse>>> getToday() {
        return ResponseEntity.ok(ApiResponse.ok(reminderService.findTodayForCurrentUser()));
    }

    @PostMapping
    @Operation(
            summary = "Crear recordatorio",
            description = "Crea un recordatorio asociado a un Lead o Customer para el usuario en sesión."
    )
    public ResponseEntity<ApiResponse<ReminderResponse>> create(
            @Valid @RequestBody ReminderRequest req) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Recordatorio creado", reminderService.create(req)));
    }

    @PatchMapping("/{id}/complete")
    @Operation(
            summary = "Marcar recordatorio como completado",
            description = "Cambia isDone a true. El recordatorio deja de aparecer en /today."
    )
    public ResponseEntity<ApiResponse<ReminderResponse>> complete(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.ok("Recordatorio completado", reminderService.complete(id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar recordatorio")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        reminderService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Recordatorio eliminado", null));
    }
}