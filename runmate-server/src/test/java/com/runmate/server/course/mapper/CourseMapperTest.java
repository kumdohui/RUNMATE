package com.runmate.server.course.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.runmate.server.course.dto.CourseSearchCondition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CourseMapperTest {
    @Autowired
    private CourseMapper courseMapper;

    @Test
    void searchesPublishedPosts() {
        CourseSearchCondition condition = new CourseSearchCondition(
                null, null, null, null, null, null, "latest", 9, 0);

        long total = courseMapper.count(condition);
        var posts = courseMapper.search(condition);

        assertThat(total).isGreaterThanOrEqualTo(posts.size());
        assertThat(posts).hasSizeLessThanOrEqualTo(9);
    }
}
