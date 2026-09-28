package com.runmate.server.admin.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminActionLogMapperTest {
    @Autowired
    private AdminActionLogMapper mapper;

    @Test
    void readsCombinedAdminHistory() {
        long total = mapper.countAll();
        var logs = mapper.searchAll(9, 0);

        assertThat(total).isGreaterThanOrEqualTo(logs.size());
        assertThat(logs).hasSizeLessThanOrEqualTo(9);
    }
}
