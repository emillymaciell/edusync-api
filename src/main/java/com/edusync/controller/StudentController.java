package com.edusync.controller;

import com.edusync.dto.request.StudentRequest;
import com.edusync.dto.request.StudentStatusRequest;
import com.edusync.dto.response.StudentProfileResponse;
import com.edusync.security.SecurityUtils;
import com.edusync.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de alunos.
 * TEACHER: cadastra e gerencia seus alunos.
 * STUDENT: consulta o próprio perfil.
 */
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    /** Professor cadastra um aluno vinculado a si. */
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<StudentProfileResponse> create(@Valid @RequestBody StudentRequest request) {
        StudentProfileResponse created = studentService.createForTeacher(SecurityUtils.currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Lista os alunos do professor autenticado. */
    @GetMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<StudentProfileResponse>> myStudents() {
        return ResponseEntity.ok(studentService.findByTeacher(SecurityUtils.currentUserId()));
    }

    @GetMapping("/{studentProfileId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<StudentProfileResponse> findById(@PathVariable Long studentProfileId) {
        return ResponseEntity.ok(studentService.findById(studentProfileId));
    }

    @PatchMapping("/{studentProfileId}/status")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<StudentProfileResponse> updateStatus(@PathVariable Long studentProfileId,
                                                               @Valid @RequestBody StudentStatusRequest request) {
        return ResponseEntity.ok(studentService.updateStatus(studentProfileId, request.status()));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentProfileResponse> myProfile() {
        return ResponseEntity.ok(studentService.getMyProfile(SecurityUtils.currentUserId()));
    }
}
