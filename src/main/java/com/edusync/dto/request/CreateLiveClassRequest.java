package com.edusync.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/** Payload para agendar uma nova aula ao vivo. */
public record CreateLiveClassRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotNull LocalDateTime scheduledDateTime,
        List<Long> studentIds
) {}
