package com.edusync.dto.response;

/**
 * Métricas da tela "Visão Geral" (Dashboard) do professor.
 */
public record DashboardMetricsResponse(
        Integer activeStudents,
        Integer tasksToCorrect,
        Integer upcomingClasses,
        Integer aiAlerts
) {}
