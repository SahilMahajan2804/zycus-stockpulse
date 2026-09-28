package com.stockpulse.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var fieldError = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError == null ? "Invalid request" : String.valueOf(fieldError.getDefaultMessage());
        String field = fieldError == null ? "" : fieldError.getField();
        String code = field.equals("quantity") ? "INVALID_QUANTITY" : field.equals("stockLevel") ? "INVALID_STOCK" :
            field.equals("currentPrice") ? "INVALID_PRICE" : "INVALID_REQUEST";
        return error(HttpStatus.BAD_REQUEST, code, message, request);
    }

    @ExceptionHandler({ProductNotFoundException.class, SuggestionNotFoundException.class})
    public ResponseEntity<Map<String, Object>> notFound(RuntimeException ex, HttpServletRequest request) {
        String code = ex instanceof ProductNotFoundException ? "PRODUCT_NOT_FOUND" : "SUGGESTION_NOT_FOUND";
        return error(HttpStatus.NOT_FOUND, code, ex.getMessage(), request);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> insufficient(InsufficientStockException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidSuggestionStateException.class)
    public ResponseEntity<Map<String, Object>> invalidState(InvalidSuggestionStateException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "INVALID_SUGGESTION_STATE", ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> duplicate(DataIntegrityViolationException ex, HttpServletRequest request) {
        String detail = String.valueOf(ex.getMessage()).toLowerCase();
        boolean suggestion = detail.contains("dedupe") || detail.contains("uk_pricing") || detail.contains("uk_reorder");
        return error(HttpStatus.CONFLICT, suggestion ? "DUPLICATE_PENDING_SUGGESTION" : "INVALID_REQUEST",
            suggestion ? "A matching pending suggestion already exists" : "A record with a unique field already exists", request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> concurrentUpdate(OptimisticLockingFailureException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "INVALID_REQUEST", "Product changed concurrently; retry the request", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> invalid(IllegalArgumentException ex, HttpServletRequest request) {
        String message = ex.getMessage() == null ? "Invalid request" : ex.getMessage();
        String lower = message.toLowerCase();
        String code = lower.contains("quantity") ? "INVALID_QUANTITY" : lower.contains("stock") ? "INVALID_STOCK" :
                lower.contains("price") ? "INVALID_PRICE" : "INVALID_REQUEST";
        return error(HttpStatus.BAD_REQUEST, code, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request body is invalid", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("code", code);
        body.put("message", message);
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
