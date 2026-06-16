package com.solventa.solventa_backend.dashboard.controller;

import com.solventa.solventa_backend.dashboard.dto.DashboardStatsResponse;
import com.solventa.solventa_backend.dashboard.service.DashboardService;
import com.solventa.solventa_backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Métricas y resumen ejecutivo del negocio")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(
            summary = "Obtener métricas del dashboard",
            description = """
            Retorna el resumen ejecutivo del tenant:
            - totalLeads / totalCustomers: conteo de registros activos
            - openQuotesCount / openQuotesValue: cotizaciones en DRAFT o SENT (pipeline abierto)
            - todayReminders: recordatorios pendientes de hoy para el usuario en sesión
            - quotesByStatus: count y suma de total por cada uno de los 5 estados de cotización
            """
    )
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getStats()));
    }
}