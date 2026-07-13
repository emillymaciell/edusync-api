package com.edusync.dto.response;

/**
 * Etapa da trilha de aprendizado do aluno.
 * {@code status}: {@code concluido}, {@code atual} ou {@code bloqueado}.
 */
public record StepDTO(
        String name,
        String status
) {}
