package com.edusync.exception;

/** Falha ao integrar/parsear a resposta da IA (Gemini). Resulta em HTTP 502. */
public class AiIntegrationException extends RuntimeException {
    public AiIntegrationException(String message) {
        super(message);
    }

    public AiIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
