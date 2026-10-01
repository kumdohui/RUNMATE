package com.runmate.server.auth.domain;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RejoinBlock {
    private Long rejoinBlockId;
    private String phoneIdentifierHash;
    private LocalDateTime blockedUntil;
    private LocalDateTime createdAt;

    public static RejoinBlock create(
            String phoneIdentifierHash,
            LocalDateTime blockedUntil
    ) {
        RejoinBlock block = new RejoinBlock();
        block.phoneIdentifierHash = phoneIdentifierHash;
        block.blockedUntil = blockedUntil;
        return block;
    }
}
