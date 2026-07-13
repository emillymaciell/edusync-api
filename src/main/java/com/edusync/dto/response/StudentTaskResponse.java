package com.edusync.dto.response;

import com.edusync.domain.enums.TaskStatus;
import com.edusync.entity.Task;

import java.time.LocalDateTime;

/**
 * Resposta de {@code GET /api/tasks/student} — tarefas do aluno autenticado.
 * Quando o aluno já enviou a atividade, {@code studentAnswer} traz as
 * alternativas marcadas (JSON {@code {"0":"opção","1":"opção",...}}).
 */
public record StudentTaskResponse(
        Long id,
        String title,
        String description,
        LocalDateTime dueDate,
        TaskStatus status,
        String studentAnswer
) {
    public static StudentTaskResponse from(Task task) {
        return new StudentTaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDueDate(),
                task.getStatus(),
                task.getStudentAnswer()
        );
    }
}
