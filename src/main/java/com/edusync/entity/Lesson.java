package com.edusync.entity;

import com.edusync.domain.enums.LessonStatus;
import com.edusync.domain.enums.LessonType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Aula criada por um professor. N:1 com {@link TeacherProfile} e N:N com {@link StudentProfile}
 * (alunos matriculados/convidados para a aula).
 */
@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String summary;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LessonType type;

    /** Link da aula (sala ao vivo ou vídeo gravado). */
    private String link;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LessonStatus status = LessonStatus.AGENDADA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_profile_id", nullable = false)
    private TeacherProfile teacher;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "lesson_students",
            joinColumns = @JoinColumn(name = "lesson_id"),
            inverseJoinColumns = @JoinColumn(name = "student_profile_id")
    )
    @Builder.Default
    private Set<StudentProfile> students = new HashSet<>();
}
