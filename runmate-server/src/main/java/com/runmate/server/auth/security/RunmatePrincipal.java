package com.runmate.server.auth.security;

import java.io.Serializable;

public record RunmatePrincipal(Long userId, String loginId, String nickname, String role, String profileImageUrl) implements Serializable {
}
