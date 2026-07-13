package com.edusync.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Parâmetros vindos do front-end para geração de exercícios via IA (Gemini).
 */
public record ExerciseGenerationRequest(
        @NotBlank String assunto,
        @NotBlank String nivelAluno,
        @NotBlank String tipoExercicio
) {}
