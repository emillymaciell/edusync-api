package com.edusync.dto.request;

import com.edusync.config.deserializer.FlexibleContentDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Payload para atualizar uma aula gerada na "Fábrica de Aulas".
 * Pelo menos um dos campos deve ser informado.
 */
public record UpdateLessonRequest(
        String topic,
        @JsonDeserialize(using = FlexibleContentDeserializer.class)
        String content
) {}
