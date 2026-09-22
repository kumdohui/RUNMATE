package com.runmate.server.auth.mapper;

import java.time.LocalDateTime;
import java.util.Optional;

import com.runmate.server.auth.domain.PhoneVerification;
import com.runmate.server.auth.domain.PhoneVerificationPurpose;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PhoneVerificationMapper {
    Optional<PhoneVerification> findById(Long verificationId);
    Optional<PhoneVerification> findTopByPhoneNumberAndPurposeOrderByCreatedAtDesc(
            @Param("phoneNumber") String phoneNumber,
            @Param("purpose") PhoneVerificationPurpose purpose
    );

    long countByPhoneNumberAndPurposeAndCreatedAtGreaterThanEqual(
            @Param("phoneNumber") String phoneNumber,
            @Param("purpose") PhoneVerificationPurpose purpose,
            @Param("createdAt") LocalDateTime createdAt
    );

    int insert(PhoneVerification verification);
    int update(PhoneVerification verification);

    default PhoneVerification save(PhoneVerification verification) {
        if (verification.getVerificationId() == null) insert(verification);
        else update(verification);
        return verification;
    }
}


