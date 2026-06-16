package com.solventa.solventa_backend.shared.util;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public class SecurityUtils {

    private SecurityUtils() {}

    // ── Obtiene el ID del usuario autenticado desde el JWT ────────────────────
    public static UUID getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("No hay usuario autenticado en el contexto");
        }

        return UUID.fromString(auth.getName());
    }
}