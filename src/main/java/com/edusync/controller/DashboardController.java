package com.edusync.controller;

import com.edusync.dto.response.DashboardMetricsResponse;
import com.edusync.security.SecurityUtils;
import com.edusync.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints da tela "Visão Geral" (Dashboard) do professor.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** Métricas agregadas do professor autenticado. */
    @GetMapping("/metrics")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<DashboardMetricsResponse> metrics() {
        return ResponseEntity.ok(dashboardService.getMetrics(SecurityUtils.currentUserId()));
    }
}
