package com.runmate.server.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.runmate.server.auth.exception.AuthException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginRateLimitService {
    private static final long MAX_FAILURES = 5;
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    public void requireAllowed(String ipAddress) {
        String value = redisTemplate.opsForValue().get(key(ipAddress));
        if (value != null && Long.parseLong(value) >= MAX_FAILURES) {
            throw new AuthException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "로그인 시도가 잠시 제한되었습니다. 10분 후 다시 시도해 주세요."
            );
        }
    }

    public void recordFailure(String ipAddress) {
        String key = key(ipAddress);
        Long failures = redisTemplate.opsForValue().increment(key);
        if (failures != null && failures == 1) {
            redisTemplate.expire(key, BLOCK_DURATION);
        }
    }

    public void clear(String ipAddress) {
        redisTemplate.delete(key(ipAddress));
    }

    private String key(String ipAddress) {
        return "security:login:ip:" + sha256(ipAddress == null ? "unknown" : ipAddress);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }
}
