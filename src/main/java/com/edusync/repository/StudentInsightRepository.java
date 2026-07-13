package com.edusync.repository;

import com.edusync.entity.StudentInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentInsightRepository extends JpaRepository<StudentInsight, Long> {

    /** Retorna o insight mais recente de um aluno (por data de atualização). */
    Optional<StudentInsight> findFirstByStudentProfileIdOrderByUpdatedAtDesc(Long studentProfileId);

    /** Conta os insights gerados para os alunos de um professor. */
    long countByStudentProfile_Teacher_Id(Long teacherProfileId);
}
