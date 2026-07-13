package com.edusync.dto.response;

import com.edusync.domain.enums.LearningLevel;
import com.edusync.domain.enums.StudentStatus;
import com.edusync.entity.StudentProfile;

public record StudentProfileResponse(
        Long id,
        Long userId,
        String name,
        String email,
        Long teacherId,
        LearningLevel learningLevel,
        StudentStatus status
) {
    public static StudentProfileResponse from(StudentProfile s) {
        return new StudentProfileResponse(
                s.getId(),
                s.getUser().getId(),
                s.getUser().getName(),
                s.getUser().getEmail(),
                s.getTeacher() != null ? s.getTeacher().getId() : null,
                s.getLearningLevel(),
                s.getStatus()
        );
    }
}
