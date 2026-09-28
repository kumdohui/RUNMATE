package com.runmate.server.auth.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

class UserSessionServiceTest {

    @Test
    void invalidatesEverySessionOwnedByUser() {
        @SuppressWarnings("unchecked")
        FindByIndexNameSessionRepository<Session> repository =
                mock(FindByIndexNameSessionRepository.class);
        Session first = mock(Session.class);
        Session second = mock(Session.class);
        when(repository.findByPrincipalName("15"))
                .thenReturn(Map.of("session-a", first, "session-b", second));

        UserSessionService service = new UserSessionService(repository);

        service.invalidateAll(15L);

        verify(repository).deleteById("session-a");
        verify(repository).deleteById("session-b");
    }
}
