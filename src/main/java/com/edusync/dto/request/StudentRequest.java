package com.edusync.dto.request;

import com.edusync.domain.enums.LearningLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Cadastro de aluno feito por um professor autenticado.
 * A senha provisória é gerada automaticamente pelo sistema.
 * O professor responsável é inferido do contexto de segurança.
 */
public record StudentRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        LearningLevel learningLevel
) {}
