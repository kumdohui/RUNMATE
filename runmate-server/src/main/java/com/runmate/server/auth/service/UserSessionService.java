package com.runmate.server.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSessionService {
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public void invalidateAll(Long userId) {
        if (userId == null) {
            return;
        }
        sessionRepository.findByPrincipalName(userId.toString())
                .keySet()
                .forEach(sessionRepository::deleteById);
    }
}
