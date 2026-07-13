package com.edusync.controller;

import com.edusync.dto.request.CreateLessonRequest;
import com.edusync.dto.request.GenerateLessonRequest;
import com.edusync.dto.request.LessonRequest;
import com.edusync.dto.request.LessonStatusRequest;
import com.edusync.dto.request.UpdateLessonRequest;
import com.edusync.dto.response.GeneratedLessonResponse;
import com.edusync.dto.response.LessonResponse;
import com.edusync.dto.response.SavedLessonResponse;
import com.edusync.security.SecurityUtils;
import com.edusync.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de aulas.
 * TEACHER: gera, salva, lista, edita e remove aulas da Fábrica de Aulas.
 * TEACHER: agenda e gerencia aulas ao vivo/gravadas em /schedule.
 * STUDENT: visualiza as aulas agendadas em que está matriculado.
 */
@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
@Slf4j
public class LessonController {

    private final LessonService lessonService;

    /** Lista todas as aulas geradas do professor autenticado. */
    @GetMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<SavedLessonResponse>> list() {
        log.info("[GET /api/lessons] Listando aulas do professor userId={}", SecurityUtils.currentUserId());
        List<SavedLessonResponse> lessons = lessonService.findAllByTeacherId(SecurityUtils.currentUserId());
        log.info("[GET /api/lessons] Retornando {} aula(s)", lessons.size());
        return ResponseEntity.ok(lessons);
    }

    /** Fábrica de Aulas: persiste a aula gerada pela IA. */
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<SavedLessonResponse> save(@Valid @RequestBody CreateLessonRequest request) {
        log.info("[POST /api/lessons] Recebido: studentId={}, topic={}, contentLength={}",
                request.studentId(), request.topic(),
                request.content() != null ? request.content().length() : 0);
        try {
            SavedLessonResponse saved = lessonService.saveGeneratedLesson(
                    SecurityUtils.currentUserId(), request);
            log.info("[POST /api/lessons] Aula salva com id={}", saved.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("ERRO DETALHADO NO SALVAMENTO DE AULA (Controller): ", e);
            throw e;
        }
    }

    /** Atualiza tópico e/ou conteúdo de uma aula gerada. */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<SavedLessonResponse> update(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateLessonRequest request) {
        log.info("[PUT /api/lessons/{}] Atualizando aula", id);
        try {
            SavedLessonResponse updated = lessonService.update(SecurityUtils.currentUserId(), id, request);
            log.info("[PUT /api/lessons/{}] Aula atualizada", id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("ERRO DETALHADO NA ATUALIZAÇÃO DE AULA (Controller): ", e);
            throw e;
        }
    }

    /** Remove uma aula gerada do professor autenticado. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("[DELETE /api/lessons/{}] Removendo aula", id);
        try {
            lessonService.deleteById(SecurityUtils.currentUserId(), id);
            log.info("[DELETE /api/lessons/{}] Aula removida", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("ERRO DETALHADO NA EXCLUSÃO DE AULA (Controller): ", e);
            throw e;
        }
    }

    /** Fábrica de Aulas: gera material didático personalizado via IA (Gemini). */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<GeneratedLessonResponse> generate(@Valid @RequestBody GenerateLessonRequest request) {
        return ResponseEntity.ok(lessonService.generateLesson(request));
    }

    /** Agenda uma aula (fluxo legado: ao vivo/gravada com data e alunos). */
    @PostMapping("/schedule")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<LessonResponse> schedule(@Valid @RequestBody LessonRequest request) {
        LessonResponse created = lessonService.createScheduled(SecurityUtils.currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/schedule/{lessonId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<LessonResponse> updateScheduled(@PathVariable Long lessonId,
                                                            @Valid @RequestBody LessonRequest request) {
        return ResponseEntity.ok(lessonService.updateScheduled(SecurityUtils.currentUserId(), lessonId, request));
    }

    @PatchMapping("/schedule/{lessonId}/status")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<LessonResponse> updateScheduledStatus(@PathVariable Long lessonId,
                                                                  @Valid @RequestBody LessonStatusRequest request) {
        return ResponseEntity.ok(lessonService.updateScheduledStatus(
                SecurityUtils.currentUserId(), lessonId, request.status()));
    }

    @GetMapping("/schedule")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<LessonResponse>> scheduledLessons() {
        return ResponseEntity.ok(lessonService.findScheduledByTeacher(SecurityUtils.currentUserId()));
    }

    @GetMapping("/schedule/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<LessonResponse>> myEnrolledLessons() {
        return ResponseEntity.ok(lessonService.findByStudentUser(SecurityUtils.currentUserId()));
    }

    @DeleteMapping("/schedule/{lessonId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> deleteScheduled(@PathVariable Long lessonId) {
        lessonService.deleteScheduled(SecurityUtils.currentUserId(), lessonId);
        return ResponseEntity.noContent().build();
    }
}
