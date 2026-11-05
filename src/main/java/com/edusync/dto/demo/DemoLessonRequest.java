package com.edusync.dto.demo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entrada da demonstração para gerar aula. Limites próprios, separados do fluxo autenticado.
 */
public record DemoLessonRequest(
        @NotBlank @Size(max = 80) String studentName,
        @NotBlank @Size(max = 30) String learningLevel,
        @NotBlank @Size(max = 200) String topic,
        @Size(max = 500) String observations
) {}
