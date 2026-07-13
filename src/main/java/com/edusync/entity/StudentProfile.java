package com.edusync.entity;

import com.edusync.domain.enums.LearningLevel;
import com.edusync.domain.enums.StudentStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Perfil de aluno. Relaciona-se 1:1 com {@link User} e N:1 com {@link TeacherProfile}
 * (o professor responsável pelo aluno).
 */
@Entity
@Table(name = "student_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** Professor responsável pelo aluno (N:1). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_profile_id")
    private TeacherProfile teacher;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private LearningLevel learningLevel = LearningLevel.INICIANTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StudentStatus status = StudentStatus.EM_DIA;
}
