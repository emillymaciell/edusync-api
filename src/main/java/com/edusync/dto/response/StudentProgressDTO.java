package com.edusync.dto.response;

import java.util.List;

/**
 * Resumo de progresso do aluno autenticado — contrato de
 * {@code GET /api/students/progress}.
 */
public record StudentProgressDTO(
        int overallProgress,
        int modulesCompleted,
        int totalModules,
        int lessonsWatched,
        int totalLessons,
        int tasksSubmitted,
        int totalTasks,
        List<StepDTO> trilha,
        List<AchievementDTO> achievements
) {}
