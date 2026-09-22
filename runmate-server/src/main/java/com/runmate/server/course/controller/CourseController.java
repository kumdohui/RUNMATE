package com.runmate.server.course.controller;

import com.runmate.server.course.dto.CourseListResponse;
import com.runmate.server.course.dto.CourseSearchRequest;
import com.runmate.server.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;

    @GetMapping
    public CourseListResponse list(@ModelAttribute CourseSearchRequest request) {
        return courseService.list(request);
    }
}
