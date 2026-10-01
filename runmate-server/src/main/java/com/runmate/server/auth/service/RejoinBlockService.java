package com.runmate.server.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.runmate.server.auth.domain.RejoinBlock;
import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.mapper.RejoinBlockMapper;

@Service
public class RejoinBlockService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long BLOCK_DAYS = 30;

    private final RejoinBlockMapper rejoinBlockMapper;
    private final SecretKeySpec secretKey;

    public RejoinBlockService(
            RejoinBlockMapper rejoinBlockMapper,
            @Value("${security.rejoin-hmac-secret}") String secret
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("재가입 제한 해시 비밀키가 설정되지 않았습니다.");
        }
        this.rejoinBlockMapper = rejoinBlockMapper;
        this.secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                HMAC_ALGORITHM
        );
    }

    public void block(String phoneNumber, LocalDateTime now) {
        rejoinBlockMapper.upsert(RejoinBlock.create(
                hash(phoneNumber),
                now.plusDays(BLOCK_DAYS)
        ));
    }

    public void requireRejoinAllowed(String phoneNumber, LocalDateTime now) {
        if (rejoinBlockMapper.existsActive(hash(phoneNumber), now)) {
            throw new AuthException("회원 탈퇴 후 30일 동안은 동일한 휴대전화 번호로 재가입할 수 없습니다.");
        }
    }

    private String hash(String phoneNumber) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(secretKey);
            return HexFormat.of().formatHex(
                    mac.doFinal(phoneNumber.getBytes(StandardCharsets.UTF_8))
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("재가입 제한 식별자를 생성하지 못했습니다.", exception);
        }
    }
}
