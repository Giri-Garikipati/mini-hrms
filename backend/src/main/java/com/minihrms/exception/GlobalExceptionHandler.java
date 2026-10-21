package com.minihrms.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Graceful failure handling in ONE place.
 * Any exception thrown in a controller or service ends up here and becomes a clean,
 * consistent JSON error like:
 *   { "status": 404, "error": "Not Found", "message": "Employee not found", ... }
 * The user never sees a Java stack trace, and the frontend always knows where to find "message".
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** The shape of every error response. */
    public record ApiError(int status, String error, String message,
                           Map<String, String> fieldErrors, Instant timestamp) { }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, Map<String, String> fieldErrors) {
        ApiError body = new ApiError(status.value(), status.getReasonPhrase(), message, fieldErrors, Instant.now());
        return ResponseEntity.status(status).body(body);
    }

    // Our own expected errors (404, 403, 409...)
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex) {
        return build(ex.getStatus(), ex.getMessage(), null);
    }

    // @Valid failed, e.g. email is blank -> 400 with which fields are wrong
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fields.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        String message = fields.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, message, fields);
    }

    // Body is not valid JSON, or a date/enum has the wrong format
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "The request body is invalid or has a wrong format", null);
    }

    // @PreAuthorize said no
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to do this", null);
    }

    // Two people saved the same row at the same time (see @Version)
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleConflict(ObjectOptimisticLockingFailureException ex) {
        return build(HttpStatus.CONFLICT, "This record was changed by someone else. Refresh and try again.", null);
    }

    // A database rule (UNIQUE, CHECK, foreign key) blocked the change
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "This change conflicts with existing data", null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleTooLarge(MaxUploadSizeExceededException ex) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "File is too large (maximum 2 MB)", null);
    }

    // Anything else. Spring's own web errors (404 unknown URL, 405 wrong method) keep their status;
    // real bugs become a generic 500 and the details go to the server log, not to the user.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            return build(status, status.getReasonPhrase(), null);
        }
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side. Please try again.", null);
    }
}
