package com.edusync.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/** Atualização parcial do progresso do aluno (campos nulos são ignorados). */
public record ProgressUpdateRequest(
        @DecimalMin("0.0") @DecimalMax("100.0") Double progressPercentage,
        String currentModule,
        Integer correctedTasks,
        Integer pendingTasks
) {}
