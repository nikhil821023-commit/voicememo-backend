package com.voicememo.exception;

import com.voicememo.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Generic not found
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex,
                                                       HttpServletRequest req) {
        String msg = ex.getMessage();

        // Classify the error by message content
        if (msg != null && msg.contains("not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of(404, "Not Found", msg, req.getRequestURI()));
        }
        if (msg != null && (msg.contains("expired") || msg.contains("no longer active"))) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(ErrorResponse.of(410, "Gone", msg, req.getRequestURI()));
        }
        if (msg != null && msg.contains("already exists")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponse.of(409, "Conflict", msg, req.getRequestURI()));
        }

        log.error("Unhandled exception at {}: {}", req.getRequestURI(), msg);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error",
                        "Something went wrong", req.getRequestURI()));
    }

    // Validation errors (e.g. @NotBlank failed)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        String message = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", message, req.getRequestURI()));
    }

    // File too large
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFileTooLarge(
            MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ErrorResponse.of(413, "Payload Too Large",
                        "Audio file exceeds 50MB limit", req.getRequestURI()));
    }
}