package com.runmate.server.home.mapper;

import com.runmate.server.home.dto.PopularCourseRow;
import com.runmate.server.home.dto.RecentPostRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HomeMapper {
    long countActiveMembers();

    List<PopularCourseRow> findPopularCourses();

    long countPublishedPosts();

    List<RecentPostRow> findRecentPosts(
            @Param("sort") String sort,
            @Param("limit") int limit,
            @Param("offset") int offset
    );
}
