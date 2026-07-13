package com.edusync.dto.response;

/** Conquista exibida na tela de progresso do aluno. */
public record AchievementDTO(
        String title,
        String description,
        String date,
        String icon
) {}
