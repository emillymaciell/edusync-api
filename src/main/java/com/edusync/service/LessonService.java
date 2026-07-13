package com.edusync.service;

import com.edusync.domain.enums.LessonStatus;
import com.edusync.dto.request.CreateLessonRequest;
import com.edusync.dto.request.GenerateLessonRequest;
import com.edusync.dto.request.LessonRequest;
import com.edusync.dto.request.UpdateLessonRequest;
import com.edusync.dto.response.GeneratedLessonResponse;
import com.edusync.dto.response.LessonResponse;
import com.edusync.dto.response.SavedLessonResponse;
import com.edusync.entity.GeneratedLesson;
import com.edusync.entity.Lesson;
import com.edusync.entity.StudentProfile;
import com.edusync.entity.TeacherProfile;
import com.edusync.exception.BusinessException;
import com.edusync.exception.ResourceNotFoundException;
import com.edusync.repository.GeneratedLessonRepository;
import com.edusync.repository.LessonRepository;
import com.edusync.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Regras de agendamento e consulta de aulas. */
@Service
@RequiredArgsConstructor
@Slf4j
public class LessonService {

    private final LessonRepository lessonRepository;
    private final GeneratedLessonRepository generatedLessonRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherService teacherService;
    private final GeminiIntegrationService geminiIntegrationService;

    /**
     * Lista todas as aulas geradas do professor autenticado.
     */
    @Transactional(readOnly = true)
    public List<SavedLessonResponse> findAllByTeacherId(Long teacherUserId) {
        TeacherProfile teacher = teacherService.findByUserId(teacherUserId);
        return generatedLessonRepository
                .findByTeacherProfileIdWithStudentOrderByCreatedAtDesc(teacher.getId())
                .stream()
                .map(gl -> SavedLessonResponse.from(gl, gl.getStudentProfile()))
                .toList();
    }

    /**
     * Atualiza tópico e/ou conteúdo de uma aula gerada.
     * Valida que o professor autenticado é o dono da aula.
     */
    @Transactional
    public SavedLessonResponse update(Long teacherUserId, Long id, UpdateLessonRequest request) {
        log.info("[LessonService.update] Iniciando atualização teacherUserId={}, lessonId={}", teacherUserId, id);
        try {
            if (!hasUpdateFields(request)) {
                throw new BusinessException("Informe ao menos o tópico ou o conteúdo para atualizar a aula.");
            }

            GeneratedLesson lesson = findOwnedGeneratedLesson(teacherUserId, id);

            if (request.topic() != null && !request.topic().isBlank()) {
                lesson.setTopic(request.topic().trim());
            }
            if (request.content() != null && !request.content().isBlank()) {
                lesson.setContent(request.content());
            }

            GeneratedLesson saved = generatedLessonRepository.save(lesson);
            log.info("[LessonService.update] Aula atualizada com id={}", saved.getId());
            return SavedLessonResponse.from(saved, saved.getStudentProfile());
        } catch (Exception e) {
            log.error("ERRO DETALHADO NA ATUALIZAÇÃO DE AULA: ", e);
            throw e;
        }
    }

    /**
     * Remove uma aula gerada.
     * Valida que o professor autenticado é o dono da aula.
     */
    @Transactional
    public void deleteById(Long teacherUserId, Long id) {
        log.info("[LessonService.deleteById] Iniciando exclusão teacherUserId={}, lessonId={}", teacherUserId, id);
        try {
            GeneratedLesson lesson = findOwnedGeneratedLesson(teacherUserId, id);
            generatedLessonRepository.delete(lesson);
            log.info("[LessonService.deleteById] Aula removida com id={}", id);
        } catch (Exception e) {
            log.error("ERRO DETALHADO NA EXCLUSÃO DE AULA: ", e);
            throw e;
        }
    }

    /**
     * Persiste uma aula gerada pela IA ("Fábrica de Aulas").
     * Valida que o aluno pertence ao professor autenticado.
     */
    @Transactional
    public SavedLessonResponse saveGeneratedLesson(Long teacherUserId, CreateLessonRequest request) {
        log.info("[LessonService.saveGeneratedLesson] Iniciando salvamento teacherUserId={}, studentId={}, topic={}",
                teacherUserId, request.studentId(), request.topic());
        try {
            TeacherProfile teacher = teacherService.findByUserId(teacherUserId);
            StudentProfile student = studentProfileRepository.findByIdWithUserAndTeacher(request.studentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado: " + request.studentId()));

            if (student.getTeacher() == null || !student.getTeacher().getId().equals(teacher.getId())) {
                throw new BusinessException("Este aluno não pertence ao professor autenticado.");
            }

            GeneratedLesson generatedLesson = GeneratedLesson.builder()
                    .topic(request.topic())
                    .content(request.content())
                    .studentProfile(student)
                    .teacherProfile(teacher)
                    .build();

            GeneratedLesson saved = generatedLessonRepository.save(generatedLesson);
            log.info("[LessonService.saveGeneratedLesson] Aula salva com id={}", saved.getId());
            return SavedLessonResponse.from(saved, student);
        } catch (Exception e) {
            log.error("ERRO DETALHADO NO SALVAMENTO DE AULA: ", e);
            throw e;
        }
    }

    /**
     * Gera material didático personalizado via IA ("Fábrica de Aulas").
     * Busca o aluno pelo ID e envia nome + nível para o Gemini.
     */
    @Transactional(readOnly = true)
    public GeneratedLessonResponse generateLesson(GenerateLessonRequest request) {
        StudentProfile student = studentProfileRepository.findById(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado: " + request.studentId()));

        String studentName = student.getUser().getName();
        String learningLevel = student.getLearningLevel() != null
                ? student.getLearningLevel().name()
                : "INICIANTE";

        return geminiIntegrationService.generateLesson(
                studentName,
                learningLevel,
                request.topic(),
                request.observations());
    }

    /** Cria uma aula agendada para o professor autenticado (fluxo legado de agenda). */
    @Transactional
    public LessonResponse createScheduled(Long teacherUserId, LessonRequest request) {
        TeacherProfile teacher = teacherService.findByUserId(teacherUserId);

        Lesson lesson = Lesson.builder()
                .title(request.title())
                .summary(request.summary())
                .scheduledAt(request.scheduledAt())
                .type(request.type())
                .link(request.link())
                .status(LessonStatus.AGENDADA)
                .teacher(teacher)
                .students(resolveStudents(request.studentIds()))
                .build();

        return LessonResponse.from(lessonRepository.save(lesson));
    }

    @Transactional
    public LessonResponse updateScheduled(Long teacherUserId, Long lessonId, LessonRequest request) {
        Lesson lesson = findOwnedScheduledLesson(teacherUserId, lessonId);
        lesson.setTitle(request.title());
        lesson.setSummary(request.summary());
        lesson.setScheduledAt(request.scheduledAt());
        lesson.setType(request.type());
        lesson.setLink(request.link());
        lesson.setStudents(resolveStudents(request.studentIds()));
        return LessonResponse.from(lessonRepository.save(lesson));
    }

    @Transactional
    public LessonResponse updateScheduledStatus(Long teacherUserId, Long lessonId, LessonStatus status) {
        Lesson lesson = findOwnedScheduledLesson(teacherUserId, lessonId);
        lesson.setStatus(status);
        return LessonResponse.from(lessonRepository.save(lesson));
    }

    @Transactional(readOnly = true)
    public List<LessonResponse> findScheduledByTeacher(Long teacherUserId) {
        TeacherProfile teacher = teacherService.findByUserId(teacherUserId);
        return lessonRepository.findByTeacherId(teacher.getId()).stream()
                .map(LessonResponse::from).toList();
    }

    /** Aulas em que o aluno autenticado está matriculado. */
    @Transactional(readOnly = true)
    public List<LessonResponse> findByStudentUser(Long studentUserId) {
        StudentProfile student = studentProfileRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de aluno não encontrado."));
        return lessonRepository.findByStudentsId(student.getId()).stream()
                .map(LessonResponse::from).toList();
    }

    @Transactional
    public void deleteScheduled(Long teacherUserId, Long lessonId) {
        Lesson lesson = findOwnedScheduledLesson(teacherUserId, lessonId);
        lessonRepository.delete(lesson);
    }

    /** Garante que a aula gerada pertence ao professor autenticado. */
    private GeneratedLesson findOwnedGeneratedLesson(Long teacherUserId, Long lessonId) {
        TeacherProfile teacher = teacherService.findByUserId(teacherUserId);
        GeneratedLesson lesson = generatedLessonRepository.findByIdWithStudentAndTeacher(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Aula não encontrada: " + lessonId));
        if (!lesson.getTeacherProfile().getId().equals(teacher.getId())) {
            throw new BusinessException("Esta aula não pertence ao professor autenticado.");
        }
        return lesson;
    }

    private static boolean hasUpdateFields(UpdateLessonRequest request) {
        return (request.topic() != null && !request.topic().isBlank())
                || (request.content() != null && !request.content().isBlank());
    }

    /** Garante que a aula agendada pertence ao professor autenticado (isolamento por tenant). */
    private Lesson findOwnedScheduledLesson(Long teacherUserId, Long lessonId) {
        TeacherProfile teacher = teacherService.findByUserId(teacherUserId);
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Aula não encontrada: " + lessonId));
        if (!lesson.getTeacher().getId().equals(teacher.getId())) {
            throw new BusinessException("Esta aula não pertence ao professor autenticado.");
        }
        return lesson;
    }

    private Set<StudentProfile> resolveStudents(Set<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<StudentProfile> students = new HashSet<>(studentProfileRepository.findAllById(studentIds));
        if (students.size() != studentIds.size()) {
            throw new ResourceNotFoundException("Um ou mais alunos informados não existem.");
        }
        return students;
    }
}
