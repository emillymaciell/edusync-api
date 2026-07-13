package com.edusync.service;

import com.edusync.domain.enums.ApprovalStatus;
import com.edusync.domain.enums.Role;
import com.edusync.dto.request.ChangePasswordRequest;
import com.edusync.dto.request.LoginRequest;
import com.edusync.dto.request.RegisterRequest;
import com.edusync.dto.response.AuthResponse;
import com.edusync.entity.Subject;
import com.edusync.entity.TeacherProfile;
import com.edusync.entity.User;
import com.edusync.exception.BusinessException;
import com.edusync.repository.SubjectRepository;
import com.edusync.repository.TeacherProfileRepository;
import com.edusync.repository.UserRepository;
import com.edusync.security.JwtUtil;
import com.edusync.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de autenticação: cadastro público (ADMIN/TEACHER) e login com emissão de JWT.
 * O cadastro de STUDENT é feito pelo {@link StudentService} (por um professor autenticado).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final SubjectRepository subjectRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    /**
     * Cadastra um novo usuário. Professores são criados com perfil PENDENTE de aprovação
     * e vinculados à matéria informada em {@code subjectId}.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() == Role.STUDENT) {
            throw new BusinessException("Alunos devem ser cadastrados por um professor.");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("E-mail já cadastrado.");
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .enabled(true)
                .build();
        user = userRepository.save(user);

        if (request.role() == Role.TEACHER) {
            Subject subject = resolveSubjectForTeacher(request.subjectId());

            TeacherProfile profile = TeacherProfile.builder()
                    .user(user)
                    .approvalStatus(ApprovalStatus.PENDENTE)
                    .build();
            profile.setSubject(subject);
            profile.getSubjects().add(subject);
            teacherProfileRepository.save(profile);
        }

        return buildAuthResponse(user);
    }

    private Subject resolveSubjectForTeacher(Long subjectId) {
        if (subjectId == null) {
            throw new BusinessException("A matéria é obrigatória para o cadastro de professor.");
        }
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new BusinessException("Matéria não encontrada: " + subjectId));
    }

    /** Autentica as credenciais e retorna um token JWT válido. */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException e) {
            throw new BusinessException("Credenciais inválidas.");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException("Credenciais inválidas."));

        return buildAuthResponse(user);
    }

    /** Troca a senha do usuário autenticado e libera o primeiro acesso. */
    @Transactional
    public void changePassword(UserPrincipal currentUser, ChangePasswordRequest request) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException("Senha atual incorreta");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setForcePasswordChange(false);
        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole().name());
        return AuthResponse.bearer(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                Boolean.TRUE.equals(user.getForcePasswordChange())
        );
    }
}
