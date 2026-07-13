package com.edusync.service;

import com.edusync.domain.enums.TaskStatus;
import com.edusync.dto.response.DashboardMetricsResponse;
import com.edusync.entity.TeacherProfile;
import com.edusync.repository.LessonRepository;
import com.edusync.repository.StudentInsightRepository;
import com.edusync.repository.StudentProfileRepository;
import com.edusync.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Agrega as métricas da tela "Visão Geral" (Dashboard) do professor.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TeacherService teacherService;
    private final StudentProfileRepository studentProfileRepository;
    private final TaskRepository taskRepository;
    private final LessonRepository lessonRepository;
    private final StudentInsightRepository studentInsightRepository;

    /**
     * Monta as métricas para o professor autenticado.
     *
     * @param teacherUserId id do {@code User} autenticado (não do TeacherProfile)
     */
    @Transactional(readOnly = true)
    public DashboardMetricsResponse getMetrics(Long teacherUserId) {
        TeacherProfile teacher = teacherService.findByUserId(teacherUserId);
        Long teacherId = teacher.getId();

        int activeStudents = (int) studentProfileRepository.countByTeacherId(teacherId);
        int tasksToCorrect = (int) taskRepository.countByLesson_Teacher_IdAndStatus(teacherId, TaskStatus.ENTREGUE);
        int upcomingClasses = (int) lessonRepository.countByTeacherIdAndScheduledAtAfter(teacherId, LocalDateTime.now());
        int aiAlerts = (int) studentInsightRepository.countByStudentProfile_Teacher_Id(teacherId);

        return new DashboardMetricsResponse(activeStudents, tasksToCorrect, upcomingClasses, aiAlerts);
    }
}
