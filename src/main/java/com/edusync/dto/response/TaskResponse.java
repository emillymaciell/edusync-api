package com.edusync.dto.response;

import com.edusync.domain.enums.TaskStatus;
import com.edusync.entity.Task;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record TaskResponse(
        Long id,
        String title,
        String description,
        LocalDateTime dueDate,
        TaskStatus status,
        Long lessonId,
        Set<Long> studentIds,
        String studentAnswer
) {
    public static TaskResponse from(Task t) {
        return new TaskResponse(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getDueDate(),
                t.getStatus(),
                t.getLesson().getId(),
                t.getStudents().stream().map(s -> s.getId()).collect(Collectors.toSet()),
                t.getStudentAnswer()
        );
    }
}
