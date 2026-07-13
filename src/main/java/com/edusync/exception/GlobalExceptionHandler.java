package com.edusync.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.hibernate.LazyInitializationException;

import java.util.HashMap;
import java.util.Map;

/** Tratamento centralizado de exceções, convertendo-as em respostas HTTP consistentes. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Recurso não encontrado: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex) {
        log.warn("Regra de negócio violada: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AiIntegrationException.class)
    public ResponseEntity<ApiError> handleAi(AiIntegrationException ex) {
        log.error("Falha na integração com IA", ex);
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Acesso negado: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Acesso negado: você não tem permissão para este recurso.");
    }

    /** JSON malformado ou tipo incompatível no @RequestBody (ex.: content como objeto inesperado). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.error("ERRO DE DESERIALIZAÇÃO DO REQUEST BODY: ", ex);
        return build(HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido. Verifique se studentId é número, title e content estão preenchidos.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Violação de integridade no banco de dados: ", ex);
        String message = "Dados inválidos para persistência.";
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("value too long")) {
            message = "O conteúdo da tarefa excede o limite da coluna no banco. "
                    + "Execute: ALTER TABLE tasks ALTER COLUMN description TYPE TEXT;";
        }
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(LazyInitializationException.class)
    public ResponseEntity<ApiError> handleLazyInit(LazyInitializationException ex) {
        log.error("LazyInitializationException: ", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro ao carregar dados relacionados. Contate o suporte técnico.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Erro de validação: {}", ex.getBindingResult().getFieldErrors());
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        ApiError body = ApiError.of(HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Erro de validação nos dados enviados.", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("ERRO NÃO TRATADO (500): ", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado.");
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(), message));
    }
}
