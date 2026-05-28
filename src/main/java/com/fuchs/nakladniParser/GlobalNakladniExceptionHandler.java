package com.fuchs.nakladniParser;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalNakladniExceptionHandler {

    @ExceptionHandler(DuplicateNakladnaException.class)
    public ResponseEntity<String> handleDuplicate(DuplicateNakladnaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}
