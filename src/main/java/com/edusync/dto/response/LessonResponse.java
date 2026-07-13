package com.edusync.dto.response;

import com.edusync.domain.enums.LessonStatus;
import com.edusync.domain.enums.LessonType;
import com.edusync.entity.Lesson;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record LessonResponse(
        Long id,
        String title,
        String summary,
        LocalDateTime scheduledAt,
        LessonType type,
        String link,
        LessonStatus status,
        Long teacherId,
        Set<Long> studentIds
) {
    public static LessonResponse from(Lesson l) {
        return new LessonResponse(
                l.getId(),
                l.getTitle(),
                l.getSummary(),
                l.getScheduledAt(),
                l.getType(),
                l.getLink(),
                l.getStatus(),
                l.getTeacher().getId(),
                l.getStudents().stream().map(s -> s.getId()).collect(Collectors.toSet())
        );
    }
}
