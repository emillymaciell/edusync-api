package com.edusync.exception;

/** Lançada quando uma entidade solicitada não existe. Resulta em HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
