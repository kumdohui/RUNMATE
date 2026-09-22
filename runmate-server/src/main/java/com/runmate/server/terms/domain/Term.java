package com.runmate.server.terms.domain;

import java.time.LocalDateTime;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Term {

    private Long termId;

    private TermCode termCode;

    private String version;

    private String title;

    private String contentUrl;

    private String contentHash;

    private Boolean required = true;

    private LocalDateTime effectiveAt;

    private LocalDateTime expiredAt;

    private LocalDateTime createdAt;

    public boolean isEffectiveAt(LocalDateTime now) {
        return !effectiveAt.isAfter(now)
                && (expiredAt == null || expiredAt.isAfter(now));
    }
}
