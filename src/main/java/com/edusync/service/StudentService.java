package com.edusync.service;

import com.edusync.domain.enums.LearningLevel;
import com.edusync.domain.enums.Role;
import com.edusync.domain.enums.StudentStatus;
import com.edusync.dto.request.StudentRequest;
import com.edusync.dto.response.StudentProfileResponse;
import com.edusync.entity.StudentProfile;
import com.edusync.entity.StudentProgress;
import com.edusync.entity.TeacherProfile;
import com.edusync.entity.User;
import com.edusync.exception.BusinessException;
import com.edusync.exception.ResourceNotFoundException;
import com.edusync.repository.StudentProfileRepository;
import com.edusync.repository.StudentProgressRepository;
import com.edusync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Gestão de alunos, sempre no contexto do professor autenticado. */
@Service
@RequiredArgsConstructor
public class StudentService {

    private static final String DEFAULT_STUDENT_PASSWORD = "edusync123";

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentProgressRepository studentProgressRepository;
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
}
