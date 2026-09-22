package com.runmate.server.terms.domain;

import java.time.LocalDateTime;


import com.runmate.server.user.domain.User;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTermAgreement {

    private Long agreementId;

    private User user;

    private Term term;

    private Boolean agreed;

    private String ipAddress;

    private String userAgent;

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
