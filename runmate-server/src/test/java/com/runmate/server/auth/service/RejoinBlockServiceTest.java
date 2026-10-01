package com.runmate.server.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.runmate.server.auth.domain.RejoinBlock;
import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.auth.mapper.RejoinBlockMapper;

@ExtendWith(MockitoExtension.class)
class RejoinBlockServiceTest {
    @Mock
    private RejoinBlockMapper rejoinBlockMapper;

    private RejoinBlockService service;

    @BeforeEach
    void setUp() {
        service = new RejoinBlockService(
                rejoinBlockMapper,
                "test-rejoin-hmac-secret"
        );
    }

    @Test
    void storesHashedPhoneForThirtyDays() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 1, 12, 0);
        ArgumentCaptor<RejoinBlock> captor = ArgumentCaptor.forClass(
                RejoinBlock.class
        );

        service.block("01012345678", now);

        verify(rejoinBlockMapper).upsert(captor.capture());
        RejoinBlock block = captor.getValue();
        assertThat(block.getPhoneIdentifierHash()).hasSize(64);
        assertThat(block.getPhoneIdentifierHash()).doesNotContain("01012345678");
        assertThat(block.getBlockedUntil()).isEqualTo(now.plusDays(30));
    }

    @Test
    void rejectsSignupWhileBlockIsActive() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 1, 12, 0);
        when(rejoinBlockMapper.existsActive(anyString(), eq(now)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.requireRejoinAllowed(
                "01012345678",
                now
        ))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("30일");
    }
}
