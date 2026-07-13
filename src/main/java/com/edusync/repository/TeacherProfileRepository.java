package com.edusync.repository;

import com.edusync.domain.enums.ApprovalStatus;
import com.edusync.entity.Subject;
import com.edusync.entity.TeacherProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, Long> {

    Optional<TeacherProfile> findByUserId(Long userId);

    Optional<TeacherProfile> findByUserEmail(String email);

    List<TeacherProfile> findByApprovalStatus(ApprovalStatus status);

    /** Conta quantos professores estão associados à matéria informada (via N:N subjects). */
    long countBySubjectsContaining(Subject subject);
}
