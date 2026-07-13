package com.edusync.entity;

import com.edusync.domain.enums.SubjectStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Área de Ensino (Matéria), gerenciada pelo ADMIN.
 */
@Entity
@Table(name = "subjects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Descrição livre da matéria (usada na tela de onboarding do professor). */
    @Column(length = 500)
    private String description;

    /** Área/categoria de ensino (ex.: "Ciências Exatas", "Linguagens"). */
    @Column(name = "teaching_area")
    private String teachingArea;

    /** Cor em formato hexadecimal para exibição no front (ex.: "#4F46E5"). */
    private String color;

    /** Identificador do ícone usado no front. */
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SubjectStatus status = SubjectStatus.ATIVA;
}
