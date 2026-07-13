package com.edusync.controller;

import com.edusync.dto.request.SubjectRequest;
import com.edusync.dto.response.SubjectResponse;
import com.edusync.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestão das Áreas de Ensino (Matérias).
 * Escrita restrita ao ADMIN; leitura pública (cadastro de professores).
 */
@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubjectResponse> create(@Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubjectResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(subjectService.update(id, request));
    }

    @GetMapping
    public ResponseEntity<List<SubjectResponse>> findAll() {
        return ResponseEntity.ok(subjectService.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<SubjectResponse>> findActive() {
        return ResponseEntity.ok(subjectService.findActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubjectResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(subjectService.findById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subjectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
