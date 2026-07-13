package com.edusync.exception;

import java.time.Instant;
import java.util.Map;

/** Estrutura padronizada de erro retornada pela API. */
public record ApiError(
        String timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now().toString(), status, error, message, null);
    }

    public static ApiError of(int status, String error, String message, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now().toString(), status, error, message, fieldErrors);
    }
}
