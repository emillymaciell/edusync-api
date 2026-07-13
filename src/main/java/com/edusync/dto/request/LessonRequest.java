package com.edusync.dto.request;

import com.edusync.domain.enums.LessonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Set;

public record LessonRequest(
        @NotBlank String title,
        String summary,
        @NotNull LocalDateTime scheduledAt,
        @NotNull LessonType type,
        String link,
        Set<Long> studentIds
) {}
