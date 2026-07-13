package com.edusync.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * DTO que mapeia diretamente o JSON retornado pela IA (Gemini) para o insight.
 * A estrutura de chaves é fixada pelo System Prompt do {@code GeminiIntegrationService}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedInsightResponse(
        String analysis,
        List<String> recommendations
) {}
