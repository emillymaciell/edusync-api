package com.edusync.dto.demo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entrada da demonstração para gerar insight. O front envia o contexto; nada é lido do banco.
 */
public record DemoInsightRequest(
        @NotBlank @Size(max = 80) String studentName,
        @Size(max = 500) String recentThemes,
        @Size(max = 800) String performanceSummary
) {}
