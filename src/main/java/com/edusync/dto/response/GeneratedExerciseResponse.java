package com.edusync.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * DTO que mapeia diretamente o JSON retornado pela IA (Gemini).
 * A estrutura de chaves é fixada pelo System Prompt do {@code GeminiIntegrationService}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedExerciseResponse(
        String tema,
        String nivel,
        List<Questao> questoes
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Questao(
            String enunciado,
            List<String> opcoes,
            String respostaCorreta,
            String explicacao
    ) {}
}
