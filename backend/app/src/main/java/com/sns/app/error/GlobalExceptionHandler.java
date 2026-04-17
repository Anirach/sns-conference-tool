package com.sns.app.error;

import com.sns.common.dto.Problem;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Problem> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::toString)
            .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", detail);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Problem> handleStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return build(status, status.getReasonPhrase(), ex.getReason());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Problem> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Problem> handleForbidden(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Problem> handleFallback(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", ex.getClass().getSimpleName());
    }

    private static ResponseEntity<Problem> build(HttpStatus status, String title, String detail) {
        return ResponseEntity.status(status).body(Problem.of(status.value(), title, detail));
    }
}
