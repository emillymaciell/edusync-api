package com.edusync.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Insight gerado por IA (Gemini) sobre o desempenho recente de um aluno.
 * Relaciona-se N:1 com {@link StudentProfile} (mantém histórico, mas na prática
 * costuma existir um registro "mais recente" por aluno).
 */
@Entity
@Table(name = "student_insights")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    /** Parágrafo de resumo/análise produzido pela IA. */
    @Column(length = 4000)
    private String analysis;

    /** Ações recomendadas ao professor (mapeadas em tabela auxiliar). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "student_insight_recommendations",
            joinColumns = @JoinColumn(name = "insight_id")
    )
    @Column(name = "recommendation", length = 1000)
    @Builder.Default
    private List<String> recommendations = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
