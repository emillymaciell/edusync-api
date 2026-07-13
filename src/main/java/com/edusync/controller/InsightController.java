package com.edusync.controller;

import com.edusync.dto.response.StudentInsightResponse;
import com.edusync.service.StudentInsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Feature "Insights IA": análise do desempenho recente do aluno pela IA (Gemini),
 * voltada ao professor. O GET aplica cache de 24h; o refresh força regeneração.
 */
@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightController {

    private final StudentInsightService studentInsightService;

    /**
     * Retorna o insight do aluno. Se o último insight tiver mais de 24h (ou não
     * existir), um novo é gerado pela IA; caso contrário, o salvo é reaproveitado.
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<StudentInsightResponse> getStudentInsight(@PathVariable Long studentId) {
        return ResponseEntity.ok(studentInsightService.getStudentInsight(studentId));
    }

    /**
     * Força a regeneração da análise via IA, sobrescrevendo o registro existente
     * (sem criar duplicata) e ignorando o cache de 24h.
     */
    @PostMapping("/{studentId}/refresh")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<StudentInsightResponse> refreshInsight(@PathVariable Long studentId) {
        return ResponseEntity.ok(studentInsightService.generateAnalysis(studentId));
    }
}
