package com.edusync.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Rastreamento de progresso do aluno (1:1 com {@link StudentProfile}).
 */
@Entity
@Table(name = "student_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false, unique = true)
    private StudentProfile student;

    /** Progresso percentual (0.0 a 100.0). */
    @Column(nullable = false)
    @Builder.Default
    private Double progressPercentage = 0.0;

    /** Módulo/etapa atual do aluno. */
    private String currentModule;

    @Column(nullable = false)
    @Builder.Default
    private Integer correctedTasks = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer pendingTasks = 0;
}
