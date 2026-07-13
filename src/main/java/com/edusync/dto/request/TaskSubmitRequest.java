package com.edusync.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Payload de submissão de atividade pelo aluno. */
public record TaskSubmitRequest(
        @NotBlank String answer
) {}
