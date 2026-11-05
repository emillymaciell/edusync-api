package com.edusync.dto.demo;

/**
 * Resposta de {@code GET /api/demo/ping}.
 *
 * @param status    sempre {@code ok} quando o endpoint responde
 * @param aiEnabled espelha {@code demo.ai.enabled}; {@code false} não impede o ping
 * @param remaining cota efetiva do IP (não reserva geração)
 */
public record DemoPingResponse(String status, boolean aiEnabled, int remaining) {}
