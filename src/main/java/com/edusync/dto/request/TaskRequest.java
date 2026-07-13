package com.edusync.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Set;

public record TaskRequest(
        @NotBlank String title,
        String description,
        LocalDateTime dueDate,
        @NotNull Long lessonId,
        Set<Long> studentIds
) {}
