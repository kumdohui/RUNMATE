package com.runmate.server.terms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.runmate.server.terms.domain.UserTermAgreement;

public interface UserTermAgreementRepository extends JpaRepository<UserTermAgreement, Long> {
    
}

