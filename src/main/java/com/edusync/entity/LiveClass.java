package com.edusync.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Aula ao vivo agendada por um professor.
 * Relaciona-se N:1 com {@link TeacherProfile} e N:N com {@link StudentProfile}.
 */
@Entity
@Table(name = "live_classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "scheduled_date_time", nullable = false)
    private LocalDateTime scheduledDateTime;

    /** Quando true, a aula é considerada finalizada independentemente da data. */
    @Builder.Default
    @Column(nullable = false)
    private boolean completed = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_profile_id", nullable = false)
    private TeacherProfile teacher;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "live_class_students",
            joinColumns = @JoinColumn(name = "live_class_id"),
            inverseJoinColumns = @JoinColumn(name = "student_profile_id")
    )
    @Builder.Default
    private Set<StudentProfile> students = new HashSet<>();
}
