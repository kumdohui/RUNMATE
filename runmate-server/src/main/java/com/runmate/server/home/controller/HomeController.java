package com.runmate.server.home.controller;

import com.runmate.server.home.dto.HomeSummaryResponse;
import com.runmate.server.home.service.HomeSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {
    private final HomeSummaryService homeSummaryService;

    @GetMapping("/summary")
    public HomeSummaryResponse getSummary(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        return homeSummaryService.getSummary(page, sort);
    }
}
