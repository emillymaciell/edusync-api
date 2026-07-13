package com.edusync.dto.response;

import com.edusync.entity.StudentInsight;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resposta da feature "Insights IA" consumida pelo front do professor.
 */
public record StudentInsightResponse(
        Long studentId,
        String module,
        String status,
        String analysis,
        List<String> recommendations,
        LocalDateTime updatedAt
) {
    public static StudentInsightResponse from(StudentInsight insight, String module, String status) {
        return new StudentInsightResponse(
                insight.getStudentProfile().getId(),
                module,
                status,
                insight.getAnalysis(),
                List.copyOf(insight.getRecommendations()),
                insight.getUpdatedAt()
        );
    }
}
