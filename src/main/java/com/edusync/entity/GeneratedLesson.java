package com.edusync.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Aula gerada pela "Fábrica de Aulas" (conteúdo produzido via IA).
 * Separada da entidade {@link Lesson}, que representa aulas agendadas (ao vivo/gravadas).
 */
@Entity
@Table(name = "generated_lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    /** Conteúdo completo gerado pela IA (texto ou JSON serializado). */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_profile_id", nullable = false)
    private TeacherProfile teacherProfile;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
