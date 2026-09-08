package com.runmate.server.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.runmate.server.auth.domain.EmailVerification;
import com.runmate.server.auth.domain.EmailVerificationPurpose;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findTopByEmailAndPurposeOrderByCreatedAtDesc(
            String email,
            EmailVerificationPurpose purpose
    );

    long countByEmailAndPurposeAndCreatedAtGreaterThanEqual(
            String email,
            EmailVerificationPurpose purpose,
            LocalDateTime createdAt
    );
}
