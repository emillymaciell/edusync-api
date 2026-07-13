package com.edusync.service;

import com.edusync.domain.enums.ApprovalStatus;
import com.edusync.dto.request.TeacherApprovalRequest;
import com.edusync.dto.request.TeacherSubjectsRequest;
import com.edusync.dto.response.TeacherProfileResponse;
import com.edusync.entity.Subject;
import com.edusync.entity.TeacherProfile;
import com.edusync.exception.ResourceNotFoundException;
import com.edusync.repository.SubjectRepository;
import com.edusync.repository.TeacherProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Regras de negócio dos professores: aprovação (ADMIN) e associação de matérias (TEACHER). */
@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherProfileRepository teacherProfileRepository;
    private final SubjectRepository subjectRepository;

    /** ADMIN aprova ou rejeita o cadastro de um professor. */
    @Transactional
    public TeacherProfileResponse review(Long teacherProfileId, TeacherApprovalRequest request) {
        TeacherProfile profile = findEntity(teacherProfileId);
        profile.setApprovalStatus(request.status());
        return TeacherProfileResponse.from(teacherProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public List<TeacherProfileResponse> findAll() {
        return teacherProfileRepository.findAll().stream().map(TeacherProfileResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherProfileResponse> findByStatus(ApprovalStatus status) {
        return teacherProfileRepository.findByApprovalStatus(status).stream()
                .map(TeacherProfileResponse::from).toList();
    }

    /** Retorna o perfil do professor autenticado (a partir do id do usuário logado). */
    @Transactional(readOnly = true)
    public TeacherProfileResponse getMyProfile(Long userId) {
        return TeacherProfileResponse.from(findByUserId(userId));
    }

    /** Professor autenticado escolhe/atualiza as matérias que leciona (N:N). */
    @Transactional
    public TeacherProfileResponse setSubjects(Long userId, TeacherSubjectsRequest request) {
        TeacherProfile profile = findByUserId(userId);
        Set<Subject> subjects = new HashSet<>(subjectRepository.findAllById(request.subjectIds()));
        if (subjects.size() != request.subjectIds().size()) {
            throw new ResourceNotFoundException("Uma ou mais matérias informadas não existem.");
        }
        profile.setSubjects(subjects);
        return TeacherProfileResponse.from(teacherProfileRepository.save(profile));
    }

    public TeacherProfile findEntity(Long teacherProfileId) {
        return teacherProfileRepository.findById(teacherProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado: " + teacherProfileId));
    }

    public TeacherProfile findByUserId(Long userId) {
        return teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de professor não encontrado para o usuário: " + userId));
    }
}
