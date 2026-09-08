package com.runmate.server.terms.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "terms")
public class Term {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "term_id")
    private Long termId;

    @Enumerated(EnumType.STRING)
    @Column(name = "term_code", nullable = false)
    private TermCode termCode;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content_url", nullable = false, length = 1000)
    private String contentUrl;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "is_required", nullable = false)
    private Boolean required = true;

    @Column(name = "effective_at", nullable = false)
    private LocalDateTime effectiveAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isEffectiveAt(LocalDateTime now) {
        return !effectiveAt.isAfter(now)
                && (expiredAt == null || expiredAt.isAfter(now));
    }
}