package com.edusync.entity;

import com.edusync.domain.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Perfil de professor (assinante do SaaS). Relaciona-se 1:1 com {@link User}
 * e N:N com {@link Subject} (matérias que o professor leciona).
 */
@Entity
@Table(name = "teacher_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Relacionamento 1:1 com o usuário dono deste perfil. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDENTE;

    /** Matéria escolhida no cadastro público do professor. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    /** Breve biografia/apresentação do professor. */
    @Column(length = 1000)
    private String bio;

    /** Matérias associadas ao professor (N:N com Subject). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "teacher_subjects",
            joinColumns = @JoinColumn(name = "teacher_profile_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    @Builder.Default
    private Set<Subject> subjects = new HashSet<>();
}
