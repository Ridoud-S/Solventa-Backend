package com.solventa.solventa_backend.customers.controller;

import com.solventa.solventa_backend.customers.dto.CustomerRequest;
import com.solventa.solventa_backend.customers.dto.CustomerResponse;
import com.solventa.solventa_backend.customers.service.CustomerService;
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
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Gestión de clientes")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @Operation(
            summary = "Listar clientes",
            description = "Retorna clientes paginados del tenant. Filtro opcional: q (búsqueda por nombre/empresa/email)."
    )
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(customerService.findAll(q, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cliente por ID")
    public ResponseEntity<ApiResponse<CustomerResponse>> getById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.ok(customerService.findById(id)));
    }

    @PostMapping
    @Operation(
            summary = "Crear cliente",
            description = "Crea un cliente directamente (sin pasar por Lead). Valida email único por tenant."
    )
    public ResponseEntity<ApiResponse<CustomerResponse>> create(
            @Valid @RequestBody CustomerRequest req) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Cliente creado", customerService.create(req)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar cliente")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerRequest req) {

        return ResponseEntity.ok(
                ApiResponse.ok("Cliente actualizado", customerService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar cliente",
            description = "Soft delete — el registro se conserva en base de datos para auditoría."
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id) {

        customerService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Cliente eliminado", null));
    }
}