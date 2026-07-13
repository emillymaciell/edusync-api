package com.edusync.controller;

import com.edusync.dto.request.CreateTaskRequest;
import com.edusync.dto.request.ExerciseGenerationRequest;
import com.edusync.dto.request.TaskStatusRequest;
import com.edusync.dto.request.TaskSubmitRequest;
import com.edusync.dto.response.GeneratedExerciseResponse;
import com.edusync.dto.response.StudentTaskResponse;
import com.edusync.dto.response.TaskListItemResponse;
import com.edusync.dto.response.TaskResponse;
import com.edusync.security.SecurityUtils;
import com.edusync.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de tarefas.
 * TEACHER: gera, salva, lista e corrige tarefas.
 * STUDENT: visualiza suas tarefas.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;

    /** Lista as tarefas dos alunos do professor autenticado. */
    @GetMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<TaskListItemResponse>> list() {
        return ResponseEntity.ok(taskService.findByTeacher(SecurityUtils.currentUserId()));
    }

    /** Persiste uma tarefa gerada pela IA (modal "Nova Tarefa"). */
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TaskListItemResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        log.info("[POST /api/tasks] Recebido: studentId={}, title={}, contentLength={}",
                request.studentId(), request.title(),
                request.content() != null ? request.content().length() : 0);
        try {
            TaskListItemResponse created = taskService.create(SecurityUtils.currentUserId(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            log.error("ERRO DETALHADO NO SALVAMENTO DE TAREFA (Controller): ", e);
            throw e;
        }
    }

    /** Gera exercícios via IA (Gemini) para o modal "Nova Tarefa". */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<GeneratedExerciseResponse> generate(@Valid @RequestBody ExerciseGenerationRequest request) {
        return ResponseEntity.ok(taskService.generateExercises(request));
    }

    @PatchMapping("/{taskId}/status")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TaskResponse> updateStatus(@PathVariable Long taskId,
                                                     @Valid @RequestBody TaskStatusRequest request) {
        return ResponseEntity.ok(taskService.updateStatus(taskId, request.status()));
    }

    /** Libera a correção de uma tarefa já entregue pelo aluno. */
    @PutMapping("/{id}/release")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TaskListItemResponse> releaseCorrection(@PathVariable Long id) {
        return ResponseEntity.ok(
                taskService.releaseTaskCorrection(id, SecurityUtils.currentUserId()));
    }

    @GetMapping("/lesson/{lessonId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<TaskResponse>> byLesson(@PathVariable Long lessonId) {
        return ResponseEntity.ok(taskService.findByLesson(lessonId));
    }

    /** Lista as tarefas do aluno autenticado (rota consumida pelo front-end Angular). */
    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentTaskResponse>> studentTasks() {
        return ResponseEntity.ok(taskService.getStudentTasks(SecurityUtils.currentUserId()));
    }

    /** Submissão de atividade pelo aluno (rota consumida pelo front-end Angular). */
    @PostMapping("/student/{id}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentTaskResponse> submitStudentTask(@PathVariable Long id,
                                                                   @Valid @RequestBody TaskSubmitRequest request) {
        return ResponseEntity.ok(taskService.submitTask(id, request.answer(), SecurityUtils.currentUserId()));
    }

    /** Submissão de atividade pelo aluno. */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentTaskResponse> submitTask(@PathVariable Long id,
                                                            @Valid @RequestBody TaskSubmitRequest request) {
        return ResponseEntity.ok(taskService.submitTask(id, request.answer(), SecurityUtils.currentUserId()));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<TaskResponse>> myTasks() {
        return ResponseEntity.ok(taskService.findByStudentUser(SecurityUtils.currentUserId()));
    }

    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> delete(@PathVariable Long taskId) {
        taskService.delete(taskId);
        return ResponseEntity.noContent().build();
    }
}
