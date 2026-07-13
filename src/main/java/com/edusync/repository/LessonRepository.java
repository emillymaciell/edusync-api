package com.edusync.repository;

import com.edusync.domain.enums.LessonStatus;
import com.edusync.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByTeacherId(Long teacherProfileId);

    List<Lesson> findByStudentsId(Long studentProfileId);

    long countByStudentsId(Long studentProfileId);

    long countByStudentsIdAndStatus(Long studentProfileId, LessonStatus status);

    /** Aula mais recente de um professor em que o aluno está matriculado. */
    Optional<Lesson> findFirstByTeacherIdAndStudents_IdOrderByScheduledAtDesc(
            Long teacherProfileId, Long studentProfileId);

    /** Aulas futuras de um professor (agendadas para depois de um instante). */
    long countByTeacherIdAndScheduledAtAfter(Long teacherProfileId, LocalDateTime reference);
}
