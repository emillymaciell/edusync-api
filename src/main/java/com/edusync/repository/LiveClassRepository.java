package com.edusync.repository;

import com.edusync.entity.LiveClass;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LiveClassRepository extends JpaRepository<LiveClass, Long> {

    /** Aulas do professor ordenadas pela data/hora crescente. */
    @EntityGraph(attributePaths = {"students", "students.user", "teacher", "teacher.user"})
    List<LiveClass> findByTeacherIdOrderByScheduledDateTimeAsc(Long teacherProfileId);

    /** Aulas atribuídas a um aluno, ordenadas pela data/hora crescente. */
    @EntityGraph(attributePaths = {"students", "students.user", "teacher", "teacher.user"})
    List<LiveClass> findByStudentsIdOrderByScheduledDateTimeAsc(Long studentProfileId);
}
