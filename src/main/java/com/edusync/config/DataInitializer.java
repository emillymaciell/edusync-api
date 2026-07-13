package com.edusync.config;

import com.edusync.domain.enums.Role;
import com.edusync.entity.User;
import com.edusync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Bootstrap de dados no startup: cria um usuário ADMIN inicial caso não exista.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${edusync.admin.email:admin@edusync.com}")
    private String adminEmail;

    @Value("${edusync.admin.password:admin123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        ensureAdmin();
    }

    /** Garante a existência do ADMIN inicial (idempotente). */
    private void ensureAdmin() {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        User admin = User.builder()
                .name("Administrador EduSync")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.save(admin);
        log.info("Usuário ADMIN inicial criado: {}", adminEmail);
    }
}
