package com.edusync.controller;

import com.edusync.dto.request.ProgressUpdateRequest;
import com.edusync.dto.response.StudentProgressResponse;
import com.edusync.security.SecurityUtils;
import com.edusync.service.StudentProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de progresso do aluno.
 * STUDENT: consulta o próprio progresso.
 * TEACHER/ADMIN: consultam e atualizam o progresso de um aluno.
 */
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class StudentProgressController {

    private final StudentProgressService progressService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentProgressResponse> myProgress() {
        return ResponseEntity.ok(progressService.getMyProgress(SecurityUtils.currentUserId()));
    }

    @GetMapping("/student/{studentProfileId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<StudentProgressResponse> byStudent(@PathVariable Long studentProfileId) {
        return ResponseEntity.ok(progressService.findByStudent(studentProfileId));
    }

    @PutMapping("/student/{studentProfileId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<StudentProgressResponse> update(@PathVariable Long studentProfileId,
                                                          @Valid @RequestBody ProgressUpdateRequest request) {
        return ResponseEntity.ok(progressService.update(
                studentProfileId,
                request.progressPercentage(),
                request.currentModule(),
                request.correctedTasks(),
                request.pendingTasks()));
    }
}
