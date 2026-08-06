package com.aistudio.api.advice;

import com.aistudio.domain.common.AiProviderException;
import com.aistudio.domain.common.DomainException;
import com.aistudio.shared.api.ApiError;
import com.aistudio.shared.logging.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", request, details);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomain(DomainException ex, HttpServletRequest request) {
        HttpStatus status = switch (ex.getCode()) {
            case "EMAIL_TAKEN", "PROJECT_KEY_TAKEN", "LABEL_TAKEN" -> HttpStatus.CONFLICT;
            case "INVALID_CREDENTIALS", "INVALID_TOKEN", "TOKEN_INVALID_OR_EXPIRED" -> HttpStatus.UNAUTHORIZED;
            case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "FORBIDDEN" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
        return build(status, ex.getCode(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<ApiError> handleAiProvider(AiProviderException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_GATEWAY, "AI_PROVIDER_ERROR", "AI provider request failed", request, List.of());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error", request, List.of());
    }

    private ApiError.FieldError toFieldError(FieldError error) {
        return new ApiError.FieldError(error.getField(), error.getDefaultMessage());
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            List<ApiError.FieldError> details
    ) {
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                code,
                message,
                request.getRequestURI(),
                MDC.get(RequestIdFilter.MDC_KEY),
                details
        );
        return ResponseEntity.status(status).body(body);
    }
}
