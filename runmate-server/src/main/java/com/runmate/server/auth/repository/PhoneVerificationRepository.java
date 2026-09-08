package com.runmate.server.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.runmate.server.auth.domain.PhoneVerification;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {
    
}
