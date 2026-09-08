package com.runmate.server.auth.dto;

public record SignupResponse(Long userId, boolean linkedExistingAccount) {
    
}
