package com.edusync.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO que mapeia o JSON retornado pela IA (Gemini) para a Fábrica de Aulas.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedLessonResponse(
        String generatedContent
) {}
