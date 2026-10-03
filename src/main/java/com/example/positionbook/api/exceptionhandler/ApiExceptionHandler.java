package com.example.positionbook.api.exceptionhandler;

import com.example.positionbook.exception.BadTradeEventException;
import com.example.positionbook.exception.ConflictException;
import com.example.positionbook.exception.ResourceNotFoundException;
import com.example.positionbook.exception.PositionOverflowException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors().forEach(error -> errors.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", "Correct the validation errors and retry.", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> constraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", "Correct the validation errors and retry.", request, errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ProblemDetail> methodValidation(HandlerMethodValidationException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> {
            String parameterName = result.getMethodParameter().getParameterName();
            String key = parameterName != null ? parameterName : "parameter";
            result.getResolvableErrors().forEach(error -> errors.putIfAbsent(key, error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value"));
        });
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", "Correct the validation errors and retry.", request, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> malformed(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Malformed request body", "The request body is missing, malformed, or contains an unsupported value.", request, Map.of());
    }

    @ExceptionHandler(BadTradeEventException.class)
    ResponseEntity<ProblemDetail> badEvent(BadTradeEventException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_TRADE_EVENT", "Invalid trade event", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ProblemDetail> conflict(ConflictException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "TRADE_EVENT_CONFLICT", "Trade event conflicts with existing state", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "POSITION_NOT_FOUND", "Position not found", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(PositionOverflowException.class)
    ResponseEntity<ProblemDetail> arithmetic(PositionOverflowException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "POSITION_OVERFLOW", "Position cannot be represented", "The requested position exceeds the supported numeric range.", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error", "An unexpected error occurred while processing the request.", request, Map.of());
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String title, String detail,
                                                   HttpServletRequest request, Map<String, String> errors) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(title);
        body.setType(URI.create("https://example.com/problems/" + code.toLowerCase().replace('_', '-')));
        body.setInstance(URI.create(request.getRequestURI()));
        body.setProperty("code", code);
        body.setProperty("timestamp", OffsetDateTime.now().toString());
        if (!errors.isEmpty()) {
            body.setProperty("errors", errors);
        }
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
