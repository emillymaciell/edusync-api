package com.edusync.repository;

import com.edusync.entity.StudentProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentProgressRepository extends JpaRepository<StudentProgress, Long> {

    Optional<StudentProgress> findByStudentId(Long studentProfileId);
}
