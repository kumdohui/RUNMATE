package com.runmate.server.auth.security;

import java.io.Serializable;
import java.security.Principal;

public record RunmatePrincipal(Long userId, String loginId, String nickname, String role, String profileImageUrl) implements Serializable, Principal {
    @Override
    public String getName() {
        return userId.toString();
    }
    public RunmatePrincipal withNickname(String newNickname) {
        return new RunmatePrincipal(userId, loginId, newNickname, role, profileImageUrl);
    }


    public RunmatePrincipal withProfileImageUrl(String newProfileImageUrl) {
        return new RunmatePrincipal(userId, loginId, nickname, role, newProfileImageUrl);
    }
}
