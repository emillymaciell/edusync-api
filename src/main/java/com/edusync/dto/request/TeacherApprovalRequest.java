package com.edusync.dto.request;

import com.edusync.domain.enums.ApprovalStatus;
import jakarta.validation.constraints.NotNull;

/** Decisão do ADMIN sobre o cadastro de um professor. */
public record TeacherApprovalRequest(
        @NotNull ApprovalStatus status
) {}
