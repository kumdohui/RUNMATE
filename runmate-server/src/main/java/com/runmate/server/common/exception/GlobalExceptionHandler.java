package com.runmate.server.common.exception;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.runmate.server.auth.exception.SignupException;
import com.runmate.server.auth.exception.LoginException;
import com.runmate.server.auth.exception.KakaoLoginException;
import com.runmate.server.common.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(KakaoLoginException.class)
    public ResponseEntity<ErrorResponse> handleKakaoLoginException(KakaoLoginException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(exception.getMessage()));
    }
    @ExceptionHandler(LoginException.class)
    public ResponseEntity<ErrorResponse> handleLoginException(LoginException exception) {
        return ResponseEntity.status(exception.getStatus()).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(SignupException.class)
    public ResponseEntity<ErrorResponse> handleSignupException(
            SignupException exception
    ) {
        ErrorResponse response = new ErrorResponse(
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("입력값을 확인해 주세요.");

        ErrorResponse response = new ErrorResponse(message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
}
