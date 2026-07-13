package com.edusync.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

/** Associação de matérias ao professor autenticado. */
public record TeacherSubjectsRequest(
        @NotEmpty Set<Long> subjectIds
) {}
