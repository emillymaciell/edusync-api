package com.edusync.repository;

import com.edusync.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByUserId(Long userId);

    Optional<StudentProfile> findByUserEmail(String email);

    List<StudentProfile> findByTeacherId(Long teacherProfileId);

    /** Quantidade de alunos vinculados a um professor. */
    long countByTeacherId(Long teacherProfileId);

    @Query("""
            SELECT s FROM StudentProfile s
            JOIN FETCH s.user
            LEFT JOIN FETCH s.teacher
            WHERE s.id = :id
            """)
    Optional<StudentProfile> findByIdWithUserAndTeacher(@Param("id") Long id);
}
