package com.edusync.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload da "Fábrica de Aulas": gera material didático personalizado via IA.
 */
public record GenerateLessonRequest(
        @NotNull Long studentId,
        @NotBlank String topic,
        String observations
) {}
