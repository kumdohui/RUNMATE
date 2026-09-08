package com.runmate.server.terms.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.runmate.server.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_term_agreements")
public class UserTermAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agreement_id")
    private Long agreementId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Column(name = "agreed", nullable = false)
    private Boolean agreed;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "action_at", nullable = false, updatable = false)
    private LocalDateTime actionAt;

    public static UserTermAgreement createAgreement(
            User user,
            Term term,
            String ipAddress,
            String userAgent
    ) {
        UserTermAgreement agreement = new UserTermAgreement();
        agreement.user = user;
        agreement.term = term;
        agreement.agreed = true;
        agreement.ipAddress = ipAddress;
        agreement.userAgent = userAgent;
        return agreement;
    }
}