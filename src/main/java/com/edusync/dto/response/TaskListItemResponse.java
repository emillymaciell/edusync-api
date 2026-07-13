package com.edusync.dto.response;

import com.edusync.domain.enums.TaskStatus;
import com.edusync.entity.StudentProfile;
import com.edusync.entity.Task;

import java.time.LocalDateTime;

/**
 * Item da listagem de tarefas do professor (visão por aluno).
 * Inclui o enunciado ({@code description}) e a submissão do aluno
 * ({@code studentAnswer}) quando já houver envio.
 */
public record TaskListItemResponse(
        Long id,
        String title,
        Long studentId,
        String studentName,
        String studentInitials,
        TaskStatus status,
        LocalDateTime dueDate,
        String description,
        String studentAnswer
) {
    public static TaskListItemResponse from(Task task, StudentProfile student) {
        String name = student.getUser() != null ? student.getUser().getName() : "Aluno";
        return new TaskListItemResponse(
                task.getId(),
                task.getTitle(),
                student.getId(),
                name,
                computeInitials(name),
                task.getStatus(),
                task.getDueDate(),
                task.getDescription(),
                task.getStudentAnswer()
        );
    }

    private static String computeInitials(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            String first = parts[0];
            String last = parts[parts.length - 1];
            if (!first.isEmpty() && !last.isEmpty()) {
                return (first.substring(0, 1) + last.substring(0, 1)).toUpperCase();
            }
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }
}
