package com.runmate.server.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.runmate.server.auth.domain.PhoneVerification;
import com.runmate.server.auth.domain.PhoneVerificationPurpose;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {
    Optional<PhoneVerification> findTopByPhoneNumberAndPurposeOrderByCreatedAtDesc(
            String phoneNumber,
            PhoneVerificationPurpose purpose
    );

    long countByPhoneNumberAndPurposeAndCreatedAtGreaterThanEqual(
            String phoneNumber,
            PhoneVerificationPurpose purpose,
            LocalDateTime createdAt
    );
}
