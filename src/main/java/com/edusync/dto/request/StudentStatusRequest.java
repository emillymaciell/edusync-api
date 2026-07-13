package com.edusync.dto.request;

import com.edusync.domain.enums.StudentStatus;
import jakarta.validation.constraints.NotNull;

public record StudentStatusRequest(@NotNull StudentStatus status) {}
