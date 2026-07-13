package com.edusync.dto.request;

import com.edusync.domain.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusRequest(@NotNull TaskStatus status) {}
