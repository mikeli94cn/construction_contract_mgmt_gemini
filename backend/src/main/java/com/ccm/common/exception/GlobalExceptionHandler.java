package com.ccm.common.exception;

import jakarta.persistence.OptimisticLockException;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return buildProblem(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        return buildProblem(HttpStatus.CONFLICT, "Duplicate Resource", ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleViolationException ex) {
        return buildProblem(HttpStatus.UNPROCESSABLE_ENTITY, "Business Rule Violation", ex.getMessage());
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ProblemDetail handleOptimisticLock(Exception ex) {
        return buildProblem(
                HttpStatus.CONFLICT,
                "Concurrent Modification Conflict",
                "The record was modified by another user. Please refresh and try again."
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        return buildProblem(
                HttpStatus.CONFLICT,
                "Data Integrity Conflict",
                "Operation violates a database uniqueness or referential integrity constraint."
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        return buildProblem(HttpStatus.UNAUTHORIZED, "Authentication Failed", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return buildProblem(HttpStatus.FORBIDDEN, "Access Denied", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = buildProblem(
                HttpStatus.BAD_REQUEST,
                "Validation Error",
                "One or more request fields failed validation."
        );
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    private ProblemDetail buildProblem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://ccm.enterprise/errors/" + status.value()));
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }
}
