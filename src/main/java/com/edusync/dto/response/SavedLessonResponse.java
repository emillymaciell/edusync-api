package com.edusync.dto.response;

import com.edusync.entity.GeneratedLesson;
import com.edusync.entity.StudentProfile;

import java.time.LocalDateTime;

/** Resposta após salvar ou listar uma aula gerada na Fábrica de Aulas. */
public record SavedLessonResponse(
        Long id,
        String topic,
        Long studentId,
        String studentName,
        String content,
        LocalDateTime createdAt
) {
    public static SavedLessonResponse from(GeneratedLesson lesson, StudentProfile student) {
        return new SavedLessonResponse(
                lesson.getId(),
                lesson.getTopic(),
                student.getId(),
                student.getUser() != null ? student.getUser().getName() : "Aluno",
                lesson.getContent(),
                lesson.getCreatedAt()
        );
    }
}
