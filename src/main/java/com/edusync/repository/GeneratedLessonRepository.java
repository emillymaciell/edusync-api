package com.edusync.repository;

import com.edusync.entity.GeneratedLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GeneratedLessonRepository extends JpaRepository<GeneratedLesson, Long> {

    @Query("""
            SELECT gl FROM GeneratedLesson gl
            JOIN FETCH gl.studentProfile sp
            JOIN FETCH sp.user
            WHERE gl.teacherProfile.id = :teacherProfileId
            ORDER BY gl.createdAt DESC
            """)
    List<GeneratedLesson> findByTeacherProfileIdWithStudentOrderByCreatedAtDesc(
            @Param("teacherProfileId") Long teacherProfileId);

    @Query("""
            SELECT gl FROM GeneratedLesson gl
            JOIN FETCH gl.studentProfile sp
            JOIN FETCH sp.user
            JOIN FETCH gl.teacherProfile
            WHERE gl.id = :id
            """)
    Optional<GeneratedLesson> findByIdWithStudentAndTeacher(@Param("id") Long id);

    List<GeneratedLesson> findByStudentProfileIdOrderByCreatedAtDesc(Long studentProfileId);
}
