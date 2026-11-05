package com.edusync.exception;

/**
 * Cota da demonstração esgotada para o IP ou para o teto global do dia.
 * O handler traduz isto em HTTP 429 com {@code Retry-After} em segundos.
 */
public class DemoRateLimitException extends RuntimeException {

    public static final String MESSAGE =
            "Limite de gerações da demonstração atingido. Tente novamente mais tarde.";

    private final long retryAfterSeconds;

    public DemoRateLimitException(long retryAfterSeconds) {
        super(MESSAGE);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
