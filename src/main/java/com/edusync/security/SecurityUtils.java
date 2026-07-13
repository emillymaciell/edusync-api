package com.edusync.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Acesso conveniente ao usuário autenticado no contexto de segurança. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("Nenhum usuário autenticado no contexto.");
        }
        return principal;
    }

    public static Long currentUserId() {
        return currentUser().getId();
    }

    public static String currentUsername() {
        return currentUser().getUsername();
    }
}
