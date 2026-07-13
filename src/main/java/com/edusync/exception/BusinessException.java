package com.edusync.exception;

/** Regra de negócio violada (ex.: e-mail duplicado). Resulta em HTTP 400/409. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
