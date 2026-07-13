package com.edusync.entity;

import com.edusync.domain.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Tarefa/exercício vinculado a uma {@link Lesson}. N:1 com Lesson e N:N com
 * {@link StudentProfile} (alunos aos quais a tarefa foi atribuída).
 */
@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    /** Conteúdo da tarefa (JSON das questões geradas pela IA ou descrição textual). */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Resposta enviada pelo aluno na submissão da atividade. */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "student_answer", columnDefinition = "TEXT")
    private String studentAnswer;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDENTE;

    /** Nota (0 a 10) atribuída na correção da tarefa; alimenta a análise da IA de insights. */
    private Integer score;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "task_students",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "student_profile_id")
    )
    @Builder.Default
    private Set<StudentProfile> students = new HashSet<>();
}
