package com.edusync.dto.response;

import com.edusync.domain.enums.SubjectStatus;
import com.edusync.entity.Subject;

public record SubjectResponse(
        Long id,
        String name,
        String description,
        String teachingArea,
        String color,
        String icon,
        SubjectStatus status,
        int teacherCount
) {
    /** Mapeia sem calcular a contagem de professores (teacherCount = 0). */
    public static SubjectResponse from(Subject s) {
        return from(s, 0L);
    }

    /** Mapeia incluindo a quantidade de professores associados à matéria. */
    public static SubjectResponse from(Subject s, long teacherCount) {
        return new SubjectResponse(s.getId(), s.getName(), s.getDescription(),
                s.getTeachingArea(), s.getColor(), s.getIcon(), s.getStatus(),
                (int) teacherCount);
    }
}
