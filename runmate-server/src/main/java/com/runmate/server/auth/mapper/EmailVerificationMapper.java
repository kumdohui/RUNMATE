package com.runmate.server.auth.mapper;

import java.time.LocalDateTime;
import java.util.Optional;

import com.runmate.server.auth.domain.EmailVerification;
import com.runmate.server.auth.domain.EmailVerificationPurpose;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmailVerificationMapper {
    Optional<EmailVerification> findById(Long verificationId);
    Optional<EmailVerification> findTopByEmailAndPurposeOrderByCreatedAtDesc(
            @Param("email") String email,
            @Param("purpose") EmailVerificationPurpose purpose
    );

    long countByEmailAndPurposeAndCreatedAtGreaterThanEqual(
            @Param("email") String email,
            @Param("purpose") EmailVerificationPurpose purpose,
            @Param("createdAt") LocalDateTime createdAt
    );

    int insert(EmailVerification verification);
    int update(EmailVerification verification);

    default EmailVerification save(EmailVerification verification) {
        if (verification.getVerificationId() == null) insert(verification);
        else update(verification);
        return verification;
    }
}


