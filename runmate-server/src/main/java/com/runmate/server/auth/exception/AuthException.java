package com.runmate.server.auth.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {
    private final HttpStatus status;

    public AuthException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public AuthException(String message, Throwable cause) {
        this(HttpStatus.BAD_REQUEST, message, cause);
    }

    public AuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public AuthException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
