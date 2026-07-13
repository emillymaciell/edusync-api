package com.edusync.dto.request;

import com.edusync.config.deserializer.FlexibleContentDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload para persistir uma tarefa gerada pela IA no modal "Nova Tarefa".
 *
 * O campo {@code content} aceita tanto uma String (resultado de
 * {@code JSON.stringify} no Angular) quanto um objeto JSON aninhado, evitando
 * erro de deserialização quando o front envia o formato errado.
 */
public record CreateTaskRequest(
        @NotNull Long studentId,
        @NotBlank String title,
        @NotBlank
        @JsonDeserialize(using = FlexibleContentDeserializer.class)
        String content
) {}
