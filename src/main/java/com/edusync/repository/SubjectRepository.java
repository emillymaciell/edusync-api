package com.edusync.repository;

import com.edusync.domain.enums.SubjectStatus;
import com.edusync.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findByStatus(SubjectStatus status);

    boolean existsByNameIgnoreCase(String name);
}
