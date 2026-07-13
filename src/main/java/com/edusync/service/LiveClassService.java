package com.edusync.service;

import com.edusync.dto.request.CreateLiveClassRequest;
import com.edusync.dto.response.LiveClassListResponse;
import com.edusync.dto.response.LiveClassResponseDTO;
import com.edusync.entity.LiveClass;
import com.edusync.entity.StudentProfile;
import com.edusync.entity.TeacherProfile;
import com.edusync.exception.BusinessException;
import com.edusync.exception.ResourceNotFoundException;
import com.edusync.repository.LiveClassRepository;
import com.edusync.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Regras de negócio das aulas ao vivo. */
@Service
@RequiredArgsConstructor
public class LiveClassService {

    private final LiveClassRepository liveClassRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherService teacherService;

    /**
     * Lista as aulas do professor autenticado, separadas em upcoming e finished.
     */
    @Transactional(readOnly = true)
    public LiveClassListResponse findMine(Long teacherUserId) {
        TeacherProfile teacher = teacherService.findByUserId(teacherUserId);
        List<LiveClassResponseDTO> all = liveClassRepository
                .findByTeacherIdOrderByScheduledDateTimeAsc(teacher.getId())
                .stream()
                .map(LiveClassResponseDTO::from)
                .toList();
        return splitByStatus(all);
    }

    /**
     * Lista as aulas do aluno autenticado, separadas em upcoming e finished.
     * Retorna 404 se o usuário não tiver perfil de aluno.
     */
    @Transactional(readOnly = true)
    public LiveClassListResponse findForStudent(Long studentUserId) {
        StudentProfile student = studentProfileRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de aluno não encontrado para o usuário autenticado."));

        List<LiveClassResponseDTO> all = liveClassRepository
                .findByStudentsIdOrderByScheduledDateTimeAsc(student.getId())
                .stream()
                .map(LiveClassResponseDTO::from)
                .toList();
        return splitByStatus(all);
    }

    private LiveClassListResponse splitByStatus(List<LiveClassResponseDTO> all) {
        List<LiveClassResponseDTO> upcoming = new ArrayList<>();
        List<LiveClassResponseDTO> finished = new ArrayList<>();
        for (LiveClassResponseDTO item : all) {
            if ("Finalizada".equals(item.status())) {
                finished.add(item);
            } else {
                upcoming.add(item);
            }
        }
        return new LiveClassListResponse(upcoming, finished);
    }

    /**
     * Agenda uma nova aula ao vivo vinculada ao professor autenticado e aos alunos informados.
     */
    @Transactional
    public LiveClassResponseDTO create(Long teacherUserId, CreateLiveClassRequest request) {
        TeacherProfile teacher = teacherService.findByUserId(teacherUserId);
        Set<StudentProfile> students = resolveStudents(teacher.getId(), request.studentIds());

        LiveClass liveClass = LiveClass.builder()
                .title(request.title().trim())
                .description(request.description().trim())
                .scheduledDateTime(request.scheduledDateTime())
                .completed(false)
                .teacher(teacher)
                .students(students)
                .build();

        return LiveClassResponseDTO.from(liveClassRepository.save(liveClass));
    }

    /**
     * Cancela (exclui) uma aula ao vivo do professor autenticado.
     * Valida ownership antes de deletar.
     */
    @Transactional
    public void delete(Long teacherUserId, Long liveClassId) {
        TeacherProfile teacher = teacherService.findByUserId(teacherUserId);
        LiveClass liveClass = liveClassRepository.findById(liveClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Aula ao vivo não encontrada: " + liveClassId));

        if (!liveClass.getTeacher().getId().equals(teacher.getId())) {
            throw new AccessDeniedException("Esta aula ao vivo não pertence ao professor autenticado.");
        }

        liveClass.getStudents().clear();
        liveClassRepository.delete(liveClass);
    }

    private Set<StudentProfile> resolveStudents(Long teacherProfileId, List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return new HashSet<>();
        }

        Set<Long> uniqueIds = new HashSet<>(studentIds);
        Set<StudentProfile> students = new HashSet<>(studentProfileRepository.findAllById(uniqueIds));
        if (students.size() != uniqueIds.size()) {
            throw new ResourceNotFoundException("Um ou mais alunos informados não existem.");
        }

        for (StudentProfile student : students) {
            if (student.getTeacher() == null || !student.getTeacher().getId().equals(teacherProfileId)) {
                throw new BusinessException(
                        "O aluno " + student.getId() + " não pertence ao professor autenticado.");
            }
        }
        return students;
    }
}
