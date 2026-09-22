package com.runmate.server.course.mapper;

import com.runmate.server.course.dto.CourseSearchCondition;
import com.runmate.server.course.dto.CourseSearchRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper {
    long count(CourseSearchCondition condition);

    List<CourseSearchRow> search(CourseSearchCondition condition);
}
