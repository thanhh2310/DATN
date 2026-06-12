package com.example.demo.exception;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalException {
    @ExceptionHandler(value = WebErrorConfig.class)
    ResponseEntity<ApiResponse> handlerRunTimeException(WebErrorConfig exception){
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.badRequest()
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Invalid request");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .message(message)
                        .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiResponse> handleConstraintViolationException(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .orElse("Invalid request");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .message(message)
                        .build());
    }
}
