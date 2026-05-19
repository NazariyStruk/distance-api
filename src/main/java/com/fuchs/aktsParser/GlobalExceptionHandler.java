package com.fuchs.aktsParser;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DuplicateDocumentException.class)
    public ResponseEntity<String> handleDuplicate(DuplicateDocumentException ex) {
        // Повертаємо 409 Conflict - це стандарт для ситуацій, коли ресурс вже існує
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}
