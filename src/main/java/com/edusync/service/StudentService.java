package com.edusync.service;

import com.edusync.domain.enums.LearningLevel;
import com.edusync.domain.enums.LessonStatus;
import com.edusync.domain.enums.Role;
import com.edusync.domain.enums.StudentStatus;
import com.edusync.domain.enums.TaskStatus;
import com.edusync.dto.request.StudentRequest;
import com.edusync.dto.response.AchievementDTO;
import com.edusync.dto.response.StudentProfileResponse;
import com.edusync.dto.response.StudentProgressDTO;
import com.edusync.dto.response.StepDTO;
import com.edusync.entity.StudentProfile;
import com.edusync.entity.StudentProgress;
import com.edusync.entity.TeacherProfile;
import com.edusync.entity.User;
import com.edusync.exception.BusinessException;
import com.edusync.exception.ResourceNotFoundException;
import com.edusync.repository.LessonRepository;
import com.edusync.repository.StudentProfileRepository;
import com.edusync.repository.StudentProgressRepository;
import com.edusync.repository.TaskRepository;
import com.edusync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Gestão de alunos, sempre no contexto do professor autenticado. */
@Service
@RequiredArgsConstructor
public class StudentService {

    private static final String DEFAULT_STUDENT_PASSWORD = "edusync123";
    private static final List<String> LEARNING_TRACK = List.of(
            "Fundamentos",
            "Present Simple",
            "Past Simple",
            "Present Perfect",
            "Conversação Avançada"
    );
    private static final DateTimeFormatter ACHIEVEMENT_DATE = DateTimeFormatter.ofPattern("dd/MM");

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentProgressRepository studentProgressRepository;
    private final TaskRepository taskRepository;
    private final LessonRepository lessonRepository;
    private final PasswordEncoder passwordEncoder;
    private final TeacherService teacherService;

    /**
     * Cria um aluno vinculado ao professor autenticado. Também inicializa o
     * registro de progresso (1:1) zerado.
     */
    @Transactional
    public StudentProfileResponse createForTeacher(Long teacherUserId, StudentRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("E-mail já cadastrado.");
        }
        TeacherProfile teacher = teacherService.findByUserId(teacherUserId);

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(DEFAULT_STUDENT_PASSWORD))
                .role(Role.STUDENT)
                .enabled(true)
                .build();
        user.setForcePasswordChange(true);
        user = userRepository.save(user);

        StudentProfile student = StudentProfile.builder()
                .user(user)
                .teacher(teacher)
                .learningLevel(request.learningLevel() != null ? request.learningLevel() : LearningLevel.INICIANTE)
                .status(StudentStatus.EM_DIA)
                .build();
        student = studentProfileRepository.save(student);

        StudentProgress progress = StudentProgress.builder()
                .student(student)
                .progressPercentage(0.0)
                .correctedTasks(0)
                .pendingTasks(0)
                .build();
        studentProgressRepository.save(progress);

        return StudentProfileResponse.from(student);
    }

    /** Lista os alunos do professor autenticado. */
    @Transactional(readOnly = true)
    public List<StudentProfileResponse> findByTeacher(Long teacherUserId) {
        TeacherProfile teacher = teacherService.findByUserId(teacherUserId);
        return studentProfileRepository.findByTeacherId(teacher.getId()).stream()
                .map(StudentProfileResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse findById(Long studentProfileId) {
        return StudentProfileResponse.from(findEntity(studentProfileId));
    }

    /** Perfil do próprio aluno autenticado. */
    @Transactional(readOnly = true)
    public StudentProfileResponse getMyProfile(Long userId) {
        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de aluno não encontrado para o usuário: " + userId));
        return StudentProfileResponse.from(profile);
    }

    /**
     * Monta o resumo de progresso do aluno autenticado a partir de tarefas e aulas reais.
     */
    @Transactional(readOnly = true)
    public StudentProgressDTO getMyProgress(Long userId) {
        StudentProfile student = studentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de aluno não encontrado para o usuário: " + userId));

        Long studentId = student.getId();

        int totalTasks = (int) taskRepository.countByStudentsId(studentId);
        int tasksSubmitted = (int) taskRepository.countByStudentsIdAndStatusIn(
                studentId, List.of(TaskStatus.ENTREGUE, TaskStatus.CORRIGIDA));

        int totalLessons = (int) lessonRepository.countByStudentsId(studentId);
        int lessonsWatched = (int) lessonRepository.countByStudentsIdAndStatus(
                studentId, LessonStatus.CONCLUIDA);

        int totalModules = LEARNING_TRACK.size();
        int currentModuleIndex = resolveCurrentModuleIndex(student.getLearningLevel());
        int modulesCompleted = currentModuleIndex;

        int overallProgress = computeOverallProgress(
                studentId, totalTasks, tasksSubmitted, totalLessons, lessonsWatched, modulesCompleted, totalModules);

        List<StepDTO> trilha = buildTrilha(currentModuleIndex);
        List<AchievementDTO> achievements = buildAchievements(
                tasksSubmitted, lessonsWatched, modulesCompleted);

        return new StudentProgressDTO(
                overallProgress,
                modulesCompleted,
                totalModules,
                lessonsWatched,
                totalLessons,
                tasksSubmitted,
                totalTasks,
                trilha,
                achievements
        );
    }

    @Transactional
    public StudentProfileResponse updateStatus(Long studentProfileId, StudentStatus status) {
        StudentProfile profile = findEntity(studentProfileId);
        profile.setStatus(status);
        return StudentProfileResponse.from(studentProfileRepository.save(profile));
    }

    public StudentProfile findEntity(Long studentProfileId) {
        return studentProfileRepository.findById(studentProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado: " + studentProfileId));
    }

    private int resolveCurrentModuleIndex(LearningLevel level) {
        if (level == null) {
            return 0;
        }
        return switch (level) {
            case INICIANTE -> 0;
            case INTERMEDIARIO -> 2;
            case AVANCADO -> LEARNING_TRACK.size() - 1;
        };
    }

    private int computeOverallProgress(Long studentId,
                                       int totalTasks,
                                       int tasksSubmitted,
                                       int totalLessons,
                                       int lessonsWatched,
                                       int modulesCompleted,
                                       int totalModules) {
        return studentProgressRepository.findByStudentId(studentId)
                .map(StudentProgress::getProgressPercentage)
                .filter(p -> p != null && p > 0)
                .map(p -> (int) Math.round(Math.min(100.0, Math.max(0.0, p))))
                .orElseGet(() -> {
                    double taskRatio = totalTasks == 0 ? 0.0 : (double) tasksSubmitted / totalTasks;
                    double lessonRatio = totalLessons == 0 ? 0.0 : (double) lessonsWatched / totalLessons;
                    double moduleRatio = totalModules == 0 ? 0.0 : (double) modulesCompleted / totalModules;
                    double blended = (taskRatio * 0.4) + (lessonRatio * 0.3) + (moduleRatio * 0.3);
                    return (int) Math.round(blended * 100.0);
                });
    }

    private List<StepDTO> buildTrilha(int currentIndex) {
        List<StepDTO> steps = new ArrayList<>(LEARNING_TRACK.size());
        for (int i = 0; i < LEARNING_TRACK.size(); i++) {
            String status;
            if (i < currentIndex) {
                status = "concluido";
            } else if (i == currentIndex) {
                status = "atual";
            } else {
                status = "bloqueado";
            }
            steps.add(new StepDTO(LEARNING_TRACK.get(i), status));
        }
        return steps;
    }

    private List<AchievementDTO> buildAchievements(int tasksSubmitted,
                                                   int lessonsWatched,
                                                   int modulesCompleted) {
        List<AchievementDTO> achievements = new ArrayList<>();
        String today = LocalDate.now().format(ACHIEVEMENT_DATE);

        if (modulesCompleted > 0) {
            String moduleName = LEARNING_TRACK.get(Math.min(modulesCompleted - 1, LEARNING_TRACK.size() - 1));
            achievements.add(new AchievementDTO(
                    "Módulo concluído",
                    "Finalizou " + moduleName,
                    today,
                    "trophy"
            ));
        }
        if (tasksSubmitted > 0) {
            achievements.add(new AchievementDTO(
                    "Tarefas enviadas",
                    "Enviou " + tasksSubmitted + " atividade(s)",
                    today,
                    "star"
            ));
        }
        if (lessonsWatched > 0) {
            achievements.add(new AchievementDTO(
                    "Aulas concluídas",
                    "Assistiu " + lessonsWatched + " aula(s)",
                    today,
                    "star"
            ));
        }
        return achievements;
    }
}
