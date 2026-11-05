package com.edusync.dto.demo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entrada da demonstração para gerar exercícios. Não reutiliza o DTO do fluxo autenticado.
 */
public record DemoExercisesRequest(
        @NotBlank @Size(max = 200) String assunto,
        @NotBlank @Size(max = 40) String nivelAluno,
        @NotBlank @Size(max = 40) String tipoExercicio
) {}
