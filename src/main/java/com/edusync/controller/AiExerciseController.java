package com.edusync.controller;

import com.edusync.dto.request.ExerciseGenerationRequest;
import com.edusync.dto.response.GeneratedExerciseResponse;
import com.edusync.service.GeminiIntegrationService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Motor de IA: geração de exercícios via Gemini.
 * Disponível apenas para professores (assinantes do SaaS).
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiExerciseController {

    private final GeminiIntegrationService geminiIntegrationService;

    @PostMapping("/exercises")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<GeneratedExerciseResponse> generate(
            @Valid @RequestBody ExerciseGenerationRequest request) {
        return ResponseEntity.ok(geminiIntegrationService.generateExercises(request));
    }

    /**
     * Diagnóstico: retorna a lista de modelos que a API Key atual pode acessar
     * (chama o ListModels do Gemini). Use para escolher um modelo válido e evitar 404/429.
     */
    @GetMapping("/models")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<JsonNode> listModels() {
        return ResponseEntity.ok(geminiIntegrationService.listAvailableModels());
    }
}
