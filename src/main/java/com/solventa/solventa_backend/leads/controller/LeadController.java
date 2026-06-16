package com.solventa.solventa_backend.leads.controller;

import com.solventa.solventa_backend.leads.dto.ConvertLeadResponse;
import com.solventa.solventa_backend.leads.dto.LeadRequest;
import com.solventa.solventa_backend.leads.dto.LeadResponse;
import com.solventa.solventa_backend.leads.dto.LeadStatusRequest;
import com.solventa.solventa_backend.leads.model.Lead;
import com.solventa.solventa_backend.leads.service.LeadService;
import com.solventa.solventa_backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
@Tag(name = "Leads", description = "Gestión de prospectos comerciales")
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    @Operation(
            summary = "Listar leads",
            description = "Retorna leads paginados del tenant. Filtros: status, priority, q (búsqueda por nombre/empresa/email)."
    )
    public ResponseEntity<ApiResponse<Page<LeadResponse>>> getAll(
            @RequestParam(required = false) Lead.LeadStatus   status,
            @RequestParam(required = false) Lead.LeadPriority priority,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                ApiResponse.ok(leadService.findAll(status, priority, q, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener lead por ID")
    public ResponseEntity<ApiResponse<LeadResponse>> getById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.ok(leadService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Crear lead")
    public ResponseEntity<ApiResponse<LeadResponse>> create(
            @Valid @RequestBody LeadRequest req) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Lead creado", leadService.create(req)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar lead")
    public ResponseEntity<ApiResponse<LeadResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody LeadRequest req) {

        return ResponseEntity.ok(
                ApiResponse.ok("Lead actualizado", leadService.update(id, req)));
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Cambiar estado del lead",
            description = "Transiciones válidas: NEW → CONTACTED → QUALIFIED → CONVERTED / DISCARDED. Un lead CONVERTED no puede cambiar de estado."
    )
    public ResponseEntity<ApiResponse<LeadResponse>> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody LeadStatusRequest req) {

        return ResponseEntity.ok(
                ApiResponse.ok(leadService.changeStatus(id, req)));
    }

    @PostMapping("/{id}/convert")
    @Operation(
            summary = "Convertir lead a cliente",
            description = "Crea un Customer con los datos del Lead y marca el Lead como CONVERTED. Retorna el ID del nuevo cliente."
    )
    public ResponseEntity<ApiResponse<ConvertLeadResponse>> convert(
            @PathVariable UUID id) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(leadService.convertToCustomer(id)));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar lead",
            description = "Soft delete — el registro se conserva en base de datos para auditoría."
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id) {

        leadService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Lead eliminado", null));
    }
}