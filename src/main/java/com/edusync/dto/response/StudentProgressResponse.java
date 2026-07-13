package com.edusync.dto.response;

import com.edusync.entity.StudentProgress;

public record StudentProgressResponse(
        Long id,
        Long studentId,
        Double progressPercentage,
        String currentModule,
        Integer correctedTasks,
        Integer pendingTasks
) {
    public static StudentProgressResponse from(StudentProgress p) {
        return new StudentProgressResponse(
                p.getId(),
                p.getStudent().getId(),
                p.getProgressPercentage(),
                p.getCurrentModule(),
                p.getCorrectedTasks(),
                p.getPendingTasks()
        );
    }
}
