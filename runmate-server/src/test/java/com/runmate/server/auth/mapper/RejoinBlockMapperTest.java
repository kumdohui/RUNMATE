package com.runmate.server.auth.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RejoinBlockMapperTest {
    @Autowired
    private RejoinBlockMapper mapper;

    @Test
    void checksActiveBlockWithoutExposingPhoneNumber() {
        boolean active = mapper.existsActive(
                "0".repeat(64),
                LocalDateTime.now()
        );

        assertThat(active).isFalse();
    }
}
