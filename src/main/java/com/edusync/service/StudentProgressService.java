package com.edusync.service;

import com.edusync.dto.response.StudentProgressResponse;
import com.edusync.entity.StudentProfile;
import com.edusync.entity.StudentProgress;
import com.edusync.exception.ResourceNotFoundException;
import com.edusync.repository.StudentProfileRepository;
import com.edusync.repository.StudentProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta e atualização do progresso do aluno. */
@Service
@RequiredArgsConstructor
public class StudentProgressService {

    private final StudentProgressRepository studentProgressRepository;
    private final StudentProfileRepository studentProfileRepository;

    @Transactional(readOnly = true)
    public StudentProgressResponse findByStudent(Long studentProfileId) {
        return StudentProgressResponse.from(findEntity(studentProfileId));
    }

    /** Progresso do próprio aluno autenticado. */
    @Transactional(readOnly = true)
    public StudentProgressResponse getMyProgress(Long userId) {
        StudentProfile student = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de aluno não encontrado para o usuário: " + userId));
        return StudentProgressResponse.from(findEntity(student.getId()));
    }

    @Transactional
    public StudentProgressResponse update(Long studentProfileId, Double progressPercentage,
                                          String currentModule, Integer correctedTasks, Integer pendingTasks) {
        StudentProgress progress = findEntity(studentProfileId);
        if (progressPercentage != null) progress.setProgressPercentage(progressPercentage);
        if (currentModule != null) progress.setCurrentModule(currentModule);
        if (correctedTasks != null) progress.setCorrectedTasks(correctedTasks);
        if (pendingTasks != null) progress.setPendingTasks(pendingTasks);
        return StudentProgressResponse.from(studentProgressRepository.save(progress));
    }

    private StudentProgress findEntity(Long studentProfileId) {
        return studentProgressRepository.findByStudentId(studentProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Progresso não encontrado para o aluno: " + studentProfileId));
    }
}
