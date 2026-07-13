package com.edusync.dto.request;

import com.edusync.config.deserializer.FlexibleContentDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload para persistir uma aula gerada pela IA na "Fábrica de Aulas".
 */
public record CreateLessonRequest(
        @NotNull Long studentId,
        @NotBlank String topic,
        @NotBlank
        @JsonDeserialize(using = FlexibleContentDeserializer.class)
        String content
) {}
