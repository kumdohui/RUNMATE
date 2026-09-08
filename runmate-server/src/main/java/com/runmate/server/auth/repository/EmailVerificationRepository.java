package com.runmate.server.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.runmate.server.auth.domain.EmailVerification;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    
}
