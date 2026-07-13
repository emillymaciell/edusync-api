package com.edusync.dto.response;

import com.edusync.domain.enums.ApprovalStatus;
import com.edusync.entity.Subject;
import com.edusync.entity.TeacherProfile;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public record TeacherProfileResponse(
        Long id,
        Long userId,
        String name,
        String email,
        ApprovalStatus approvalStatus,
        String bio,
        String subjectName,
        String subjectCategory,
        Set<SubjectResponse> subjects
) {
    public static TeacherProfileResponse from(TeacherProfile t) {
        Set<SubjectResponse> subjects = t.getSubjects().stream()
                .map(SubjectResponse::from)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Subject chosen = t.getSubject();
        if (subjects.isEmpty() && chosen != null) {
            subjects.add(SubjectResponse.from(chosen));
        }

        return new TeacherProfileResponse(
                t.getId(),
                t.getUser().getId(),
                t.getUser().getName(),
                t.getUser().getEmail(),
                t.getApprovalStatus(),
                t.getBio(),
                chosen != null ? chosen.getName() : null,
                chosen != null ? chosen.getTeachingArea() : null,
                subjects
        );
    }
}
