package com.runmate.server.course.controller;

import com.runmate.server.course.dto.CourseListResponse;
import com.runmate.server.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;

    @GetMapping
    public CourseListResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Double minDistance,
            @RequestParam(required = false) Double maxDistance
    ) {
        return courseService.list(page, sort, keyword, categoryId, region, tag, minDistance, maxDistance);
    }
}
