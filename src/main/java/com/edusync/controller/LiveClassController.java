package com.edusync.controller;

import com.edusync.dto.request.CreateLiveClassRequest;
import com.edusync.dto.response.LiveClassListResponse;
import com.edusync.dto.response.LiveClassResponseDTO;
import com.edusync.security.SecurityUtils;
import com.edusync.service.LiveClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de aulas ao vivo.
 * TEACHER: agenda e lista as próprias aulas.
 * STUDENT: lista as aulas em que está matriculado.
 */
@RestController
@RequestMapping("/api/live-classes")
@RequiredArgsConstructor
public class LiveClassController {

    private final LiveClassService liveClassService;

    /** Lista as aulas do professor autenticado (upcoming + finished). */
    @GetMapping("/me")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<LiveClassListResponse> myLiveClasses() {
        return ResponseEntity.ok(liveClassService.findMine(SecurityUtils.currentUserId()));
    }

    /** Lista as aulas do aluno autenticado (upcoming + finished). */
    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<LiveClassListResponse> studentLiveClasses() {
        return ResponseEntity.ok(liveClassService.findForStudent(SecurityUtils.currentUserId()));
    }

    /** Agenda uma nova aula ao vivo vinculada ao professor logado. */
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<LiveClassResponseDTO> create(@Valid @RequestBody CreateLiveClassRequest request) {
        LiveClassResponseDTO created = liveClassService.create(SecurityUtils.currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Cancela (exclui) uma aula ao vivo do professor autenticado. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        liveClassService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
