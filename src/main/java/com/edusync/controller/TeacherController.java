package com.edusync.controller;

import com.edusync.domain.enums.ApprovalStatus;
import com.edusync.dto.request.TeacherApprovalRequest;
import com.edusync.dto.request.TeacherSubjectsRequest;
import com.edusync.dto.response.TeacherProfileResponse;
import com.edusync.security.SecurityUtils;
import com.edusync.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de professores.
 * ADMIN: lista e aprova/rejeita cadastros.
 * TEACHER: consulta o próprio perfil e associa matérias.
 */
@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TeacherProfileResponse>> findAll() {
        return ResponseEntity.ok(teacherService.findAll());
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TeacherProfileResponse>> findByStatus(@PathVariable ApprovalStatus status) {
        return ResponseEntity.ok(teacherService.findByStatus(status));
    }

    /** ADMIN aprova ou rejeita o cadastro de um professor. */
    @PatchMapping("/{teacherProfileId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeacherProfileResponse> review(@PathVariable Long teacherProfileId,
                                                         @Valid @RequestBody TeacherApprovalRequest request) {
        return ResponseEntity.ok(teacherService.review(teacherProfileId, request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TeacherProfileResponse> myProfile() {
        return ResponseEntity.ok(teacherService.getMyProfile(SecurityUtils.currentUserId()));
    }

    /** Professor autenticado escolhe/atualiza as matérias que leciona. */
    @PutMapping("/me/subjects")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TeacherProfileResponse> setSubjects(@Valid @RequestBody TeacherSubjectsRequest request) {
        return ResponseEntity.ok(teacherService.setSubjects(SecurityUtils.currentUserId(), request));
    }
}
