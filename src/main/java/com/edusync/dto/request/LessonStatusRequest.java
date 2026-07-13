package com.edusync.dto.request;

import com.edusync.domain.enums.LessonStatus;
import jakarta.validation.constraints.NotNull;

public record LessonStatusRequest(@NotNull LessonStatus status) {}
