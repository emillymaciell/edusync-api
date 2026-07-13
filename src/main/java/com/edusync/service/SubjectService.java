package com.edusync.service;

import com.edusync.domain.enums.SubjectStatus;
import com.edusync.dto.request.SubjectRequest;
import com.edusync.dto.response.SubjectResponse;
import com.edusync.entity.Subject;
import com.edusync.exception.BusinessException;
import com.edusync.exception.ResourceNotFoundException;
import com.edusync.repository.SubjectRepository;
import com.edusync.repository.TeacherProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Gestão das Áreas de Ensino (Matérias) - operações exclusivas do ADMIN. */
@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final TeacherProfileRepository teacherProfileRepository;

    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        if (subjectRepository.existsByNameIgnoreCase(request.name())) {
            throw new BusinessException("Já existe uma matéria com este nome.");
        }
        Subject subject = Subject.builder()
                .name(request.name())
                .description(request.description())
                .teachingArea(request.teachingArea())
                .color(request.color())
                .icon(request.icon())
                .status(SubjectStatus.ATIVA)
                .build();
        return toResponse(subjectRepository.save(subject));
    }

    @Transactional
    public SubjectResponse update(Long id, SubjectRequest request) {
        Subject subject = findEntity(id);
        subject.setName(request.name());
        subject.setDescription(request.description());
        subject.setTeachingArea(request.teachingArea());
        subject.setColor(request.color());
        subject.setIcon(request.icon());
        if (request.status() != null) {
            subject.setStatus(request.status());
        }
        return toResponse(subjectRepository.save(subject));
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> findAll() {
        return subjectRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> findActive() {
        return subjectRepository.findByStatus(SubjectStatus.ATIVA).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SubjectResponse findById(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public void delete(Long id) {
        Subject subject = findEntity(id);
        subjectRepository.delete(subject);
    }

    public Subject findEntity(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matéria não encontrada: " + id));
    }

    /**
     * Monta o DTO já com a quantidade de professores associados à matéria.
     * Uma contagem por matéria é suficiente dado o volume esperado (poucas dezenas).
     */
    private SubjectResponse toResponse(Subject subject) {
        long teacherCount = teacherProfileRepository.countBySubjectsContaining(subject);
        return SubjectResponse.from(subject, teacherCount);
    }
}
