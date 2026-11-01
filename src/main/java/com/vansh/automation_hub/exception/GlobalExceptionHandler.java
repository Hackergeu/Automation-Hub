package com.vansh.automation_hub.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

// Saare controllers ke errors yahin pakde jaate hain aur saaf JSON mein badal jaate hain
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Job nahi mila -> 404
    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(JobNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    // Validation fail (jaise name khali) -> 400, aur batao kaun si field galat hai
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    // Galat cron -> 400
    @ExceptionHandler(InvalidCronException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCron(InvalidCronException ex) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("cronExpression", ex.getMessage()));
    }

    // Galat ya unavailable channel/target -> 400, aur batao kaun si field galat hai
    @ExceptionHandler(InvalidChannelException.class)
    public ResponseEntity<Map<String, String>> handleInvalidChannel(InvalidChannelException ex) {
        return ResponseEntity
                .badRequest()
                .body(Map.of(ex.getField(), ex.getMessage()));
    }
}