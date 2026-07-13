package com.edusync.dto.response;

import com.edusync.entity.LiveClass;
import com.edusync.entity.StudentProfile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** DTO de resposta de uma aula ao vivo, com status calculado dinamicamente. */
public record LiveClassResponseDTO(
        Long id,
        String title,
        String description,
        LocalDateTime scheduledDateTime,
        String status,
        Long teacherId,
        String teacherName,
        List<StudentSummary> students
) {
    public record StudentSummary(
            Long id,
            String name
    ) {
        public static StudentSummary from(StudentProfile student) {
            String name = student.getUser() != null ? student.getUser().getName() : "Aluno";
            return new StudentSummary(student.getId(), name);
        }
    }

    public static LiveClassResponseDTO from(LiveClass liveClass) {
        String teacherName = liveClass.getTeacher() != null
                && liveClass.getTeacher().getUser() != null
                ? liveClass.getTeacher().getUser().getName()
                : null;
        return new LiveClassResponseDTO(
                liveClass.getId(),
                liveClass.getTitle(),
                liveClass.getDescription(),
                liveClass.getScheduledDateTime(),
                resolveStatus(liveClass),
                liveClass.getTeacher() != null ? liveClass.getTeacher().getId() : null,
                teacherName,
                liveClass.getStudents().stream().map(StudentSummary::from).toList()
        );
    }

    /**
     * Status dinâmico:
     * - Finalizada: data já passou (ontem para trás) OU completed = true
     * - Em breve: data da aula = hoje
     * - Agendada: amanhã em diante
     */
    static String resolveStatus(LiveClass liveClass) {
        if (liveClass.isCompleted()) {
            return "Finalizada";
        }
        LocalDate classDate = liveClass.getScheduledDateTime().toLocalDate();
        LocalDate today = LocalDate.now();

        if (classDate.isBefore(today)) {
            return "Finalizada";
        }
        if (classDate.isEqual(today)) {
            return "Em breve";
        }
        return "Agendada";
    }
}
