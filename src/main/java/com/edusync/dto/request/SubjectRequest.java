package com.edusync.dto.request;

import com.edusync.domain.enums.SubjectStatus;
import jakarta.validation.constraints.NotBlank;

public record SubjectRequest(
        @NotBlank String name,
        String description,
        String teachingArea,
        String color,
        String icon,
        SubjectStatus status
) {}
