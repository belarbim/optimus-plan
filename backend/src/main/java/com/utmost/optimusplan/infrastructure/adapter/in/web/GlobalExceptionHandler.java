package com.utmost.optimusplan.infrastructure.adapter.in.web;

import com.utmost.optimusplan.domain.exception.DomainError;
import com.utmost.optimusplan.domain.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    record ErrorResponse(String message, List<String> details, LocalDateTime timestamp) {}

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException ex) {
        return switch (ex.error()) {
            case DomainError.NotFound e ->
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ErrorResponse(e.message(), List.of(), LocalDateTime.now()));
            case DomainError.Conflict e ->
                    ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(new ErrorResponse(e.message(), List.of(), LocalDateTime.now()));
            case DomainError.BusinessRule e ->
                    ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ErrorResponse(e.message(), List.of(), LocalDateTime.now()));
            case DomainError.Validation e ->
                    ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ErrorResponse("Validation failed", e.violations(), LocalDateTime.now()));
        };
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Validation failed", violations, LocalDateTime.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal server error", List.of(ex.getMessage()), LocalDateTime.now()));
    }
}
