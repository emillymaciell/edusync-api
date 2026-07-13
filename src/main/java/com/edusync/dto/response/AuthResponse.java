package com.edusync.dto.response;

import com.edusync.domain.enums.Role;

/** Resposta de autenticação com o token JWT e dados básicos do usuário. */
public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String name,
        String email,
        Role role,
        boolean forcePasswordChange
) {
    public static AuthResponse bearer(String token, Long userId, String name, String email, Role role,
                                      boolean forcePasswordChange) {
        return new AuthResponse(token, "Bearer", userId, name, email, role, forcePasswordChange);
    }
}
