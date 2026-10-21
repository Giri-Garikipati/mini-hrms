package com.minihrms.exception;

import org.springframework.http.HttpStatus;

/**
 * Our own exception for "expected" problems: not found, not allowed, bad input.
 * Services throw it with the right HTTP status; GlobalExceptionHandler turns it into JSON.
 * Example: throw new ApiException(HttpStatus.NOT_FOUND, "Employee not found");
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
