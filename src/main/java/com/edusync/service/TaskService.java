package com.edusync.service;

import com.edusync.domain.enums.TaskStatus;
import com.edusync.dto.request.CreateTaskRequest;
import com.edusync.dto.request.ExerciseGenerationRequest;
import com.edusync.dto.request.TaskRequest;
import com.edusync.dto.response.GeneratedExerciseResponse;
import com.edusync.dto.response.StudentTaskResponse;
import com.edusync.dto.response.TaskListItemResponse;
import com.edusync.dto.response.TaskResponse;
import com.edusync.entity.Lesson;
import com.edusync.entity.StudentProfile;
import com.edusync.entity.Task;
import com.edusync.entity.TeacherProfile;
import com.edusync.exception.BusinessException;
import com.edusync.exception.ResourceNotFoundException;
import com.edusync.repository.LessonRepository;
import com.edusync.repository.StudentProfileRepository;
import com.edusync.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Regras de criação e acompanhamento de tarefas/exercícios. */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final LessonRepository lessonRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherService teacherService;
    private final GeminiIntegrationService geminiIntegrationService;

    /** Gera exercícios via IA (Gemini) para o modal "Nova Tarefa". */
    public GeneratedExerciseResponse generateExercises(ExerciseGenerationRequest request) {
        return geminiIntegrationService.generateExercises(request);
    }

    /**
     * Lista as tarefas do professor autenticado, uma entrada por par (tarefa, aluno).
     */
    @Transactional(readOnly = true)
    public List<TaskListItemResponse> findByTeacher(Long teacherUserId) {
        TeacherProfile teacher = teacherService.findByUserId(teacherUserId);
        List<Task> tasks = taskRepository.findByTeacherWithStudents(teacher.getId());

        List<TaskListItemResponse> items = new ArrayList<>();
        for (Task task : tasks) {
            for (StudentProfile student : task.getStudents()) {
                items.add(TaskListItemResponse.from(task, student));
            }
        }
        return items;
    }

    /**
     * Persiste uma tarefa gerada pela IA: vincula ao aluno, associa à aula mais
     * recente do professor com esse aluno, status PENDENTE e prazo em 7 dias.
     */
    @Transactional
    public TaskListItemResponse create(Long teacherUserId, CreateTaskRequest request) {
        log.info("[TaskService.create] Iniciando salvamento para teacherUserId={}, studentId={}",
                teacherUserId, request.studentId());
        try {
            TeacherProfile teacher = teacherService.findByUserId(teacherUserId);
            StudentProfile student = studentProfileRepository.findByIdWithUserAndTeacher(request.studentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado: " + request.studentId()));

            if (student.getTeacher() == null || !student.getTeacher().getId().equals(teacher.getId())) {
                throw new BusinessException("Este aluno não pertence ao professor autenticado.");
            }

            Lesson lesson = lessonRepository
                    .findFirstByTeacherIdAndStudents_IdOrderByScheduledAtDesc(teacher.getId(), student.getId())
                    .orElse(null);

            Task task = Task.builder()
                    .title(request.title())
                    .description(request.content())
                    .dueDate(LocalDateTime.now().plusDays(7))
                    .status(TaskStatus.PENDENTE)
                    .lesson(lesson)
                    .students(new HashSet<>(Set.of(student)))
                    .build();

            Task saved = taskRepository.save(task);
            log.info("[TaskService.create] Tarefa salva com id={}", saved.getId());
            return TaskListItemResponse.from(saved, student);
        } catch (Exception e) {
            log.error("ERRO DETALHADO NO SALVAMENTO DE TAREFA: ", e);
            throw e;
        }
    }

    /** Criação legada com lessonId explícito (mantida para compatibilidade interna). */
    @Transactional
    public TaskResponse createWithLesson(TaskRequest request) {
        Lesson lesson = lessonRepository.findById(request.lessonId())
                .orElseThrow(() -> new ResourceNotFoundException("Aula não encontrada: " + request.lessonId()));

        Task task = Task.builder()
                .title(request.title())
                .description(request.description())
                .dueDate(request.dueDate())
                .status(TaskStatus.PENDENTE)
                .lesson(lesson)
                .students(resolveStudents(request.studentIds()))
                .build();

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateStatus(Long taskId, TaskStatus status) {
        Task task = findEntity(taskId);
        task.setStatus(status);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findByLesson(Long lessonId) {
        return taskRepository.findByLessonId(lessonId).stream().map(TaskResponse::from).toList();
    }

    /** Tarefas atribuídas ao aluno autenticado. */
    @Transactional(readOnly = true)
    public List<TaskResponse> findByStudentUser(Long studentUserId) {
        StudentProfile student = studentProfileRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de aluno não encontrado."));
        return taskRepository.findByStudentsId(student.getId()).stream().map(TaskResponse::from).toList();
    }

    /** Tarefas do aluno autenticado para `GET /api/tasks/student`. */
    @Transactional(readOnly = true)
    public List<StudentTaskResponse> getStudentTasks(Long userId) {
        studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de aluno não encontrado."));
        return taskRepository.findByStudentProfileUserId(userId).stream()
                .map(StudentTaskResponse::from)
                .toList();
    }

    /**
     * Registra a resposta do aluno e marca a tarefa como ENTREGUE.
     * Garante que a tarefa pertence ao aluno autenticado.
     */
    @Transactional
    public StudentTaskResponse submitTask(Long taskId, String answer, Long userId) {
        log.info("[TaskService.submitTask] Submissão taskId={}, userId={}", taskId, userId);
        studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de aluno não encontrado."));

        Task task = taskRepository.findByIdAndStudentProfileUserId(taskId, userId)
                .orElseThrow(() -> new BusinessException(
                        "Tarefa não encontrada ou não pertence ao aluno autenticado."));

        task.setStudentAnswer(answer);
        task.setStatus(TaskStatus.ENTREGUE);

        Task saved = taskRepository.save(task);
        log.info("[TaskService.submitTask] Tarefa {} entregue com sucesso", saved.getId());
        return StudentTaskResponse.from(saved);
    }

    /**
     * Libera a correção de uma tarefa entregue pelo aluno.
     * Valida ownership do professor e exige status ENTREGUE.
     */
    @Transactional
    public TaskListItemResponse releaseTaskCorrection(Long taskId, Long teacherUserId) {
        log.info("[TaskService.releaseTaskCorrection] taskId={}, teacherUserId={}", taskId, teacherUserId);
        TeacherProfile teacher = teacherService.findByUserId(teacherUserId);

        Task task = taskRepository.findByIdAndLesson_Teacher_Id(taskId, teacher.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tarefa não encontrada ou não pertence a este professor."));

        if (task.getStatus() != TaskStatus.ENTREGUE) {
            throw new BusinessException(
                    "Apenas tarefas com status ENTREGUE podem ter a correção liberada.");
        }

        task.setStatus(TaskStatus.CORRIGIDA);
        Task saved = taskRepository.save(task);

        StudentProfile student = saved.getStudents().stream().findFirst()
                .orElseThrow(() -> new BusinessException("Tarefa sem aluno vinculado."));

        log.info("[TaskService.releaseTaskCorrection] Tarefa {} marcada como CORRIGIDA", saved.getId());
        return TaskListItemResponse.from(saved, student);
    }

    @Transactional
    public void delete(Long taskId) {
        taskRepository.delete(findEntity(taskId));
    }

    public Task findEntity(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada: " + taskId));
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
