package com.edusync.dto.request;

import com.edusync.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload de cadastro. Professores entram com aprovação PENDENTE;
 * o papel STUDENT normalmente é criado por um professor autenticado.
 */
public record RegisterRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotNull Role role,
        Long subjectId
) {}
