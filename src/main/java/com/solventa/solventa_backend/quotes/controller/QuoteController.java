package com.solventa.solventa_backend.quotes.controller;

import com.solventa.solventa_backend.quotes.dto.*;
import com.solventa.solventa_backend.quotes.model.Quote;
import com.solventa.solventa_backend.quotes.service.QuoteService;
import com.solventa.solventa_backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
@Tag(name = "Quotes", description = "Gestión de cotizaciones")
public class QuoteController {

    private final QuoteService quoteService;

    @GetMapping
    @Operation(
            summary = "Listar cotizaciones",
            description = "Retorna cotizaciones paginadas del tenant. Filtros: status, customerId, q (búsqueda por título)."
    )
    public ResponseEntity<ApiResponse<Page<QuoteResponse>>> getAll(
            @RequestParam(required = false) Quote.QuoteStatus status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                ApiResponse.ok(quoteService.findAll(status, customerId, q, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cotización por ID", description = "Incluye líneas de detalle y datos del cliente.")
    public ResponseEntity<ApiResponse<QuoteResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(quoteService.findById(id)));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Historial de cambios de estado", description = "Lista cronológica de transiciones de estado con quién las realizó.")
    public ResponseEntity<ApiResponse<List<QuoteStatusHistoryResponse>>> getHistory(
            @PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.ok(quoteService.getStatusHistory(id)));
    }

    @PostMapping
    @Operation(
            summary = "Crear cotización",
            description = "Crea una cotización en estado DRAFT con sus líneas. Calcula subtotal y total automáticamente (descuento + IVA)."
    )
    public ResponseEntity<ApiResponse<QuoteResponse>> create(
            @Valid @RequestBody QuoteRequest req) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Cotización creada", quoteService.create(req)));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar cotización",
            description = "Solo permitido si la cotización está en estado DRAFT. Reemplaza todas las líneas."
    )
    public ResponseEntity<ApiResponse<QuoteResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody QuoteRequest req) {

        return ResponseEntity.ok(
                ApiResponse.ok("Cotización actualizada", quoteService.update(id, req)));
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Cambiar estado de la cotización",
            description = "Transiciones válidas: DRAFT→SENT, SENT→WON/LOST/EXPIRED. " +
                    "WON, LOST y EXPIRED son estados terminales. Registra el cambio en el historial."
    )
    public ResponseEntity<ApiResponse<QuoteResponse>> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody QuoteStatusRequest req) {

        return ResponseEntity.ok(ApiResponse.ok(quoteService.changeStatus(id, req)));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar cotización",
            description = "Soft delete. Solo permitido si la cotización está en estado DRAFT."
    )
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        quoteService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Cotización eliminada", null));
    }
}