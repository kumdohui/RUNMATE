package com.runmate.server.auth.exception;

import org.springframework.http.HttpStatus;

public class LoginException extends RuntimeException {
    private final HttpStatus status;

    public LoginException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
