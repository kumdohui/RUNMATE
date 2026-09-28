package com.runmate.server.user.controller;

import com.runmate.server.user.dto.PublicProfileDtos.PublicPostFeedResponse;
import com.runmate.server.user.dto.PublicProfileDtos.PublicProfileResponse;
import com.runmate.server.user.service.PublicProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PublicProfileController {
    private final PublicProfileService publicProfileService;

    @GetMapping("/api/users/{userId:\\d+}")
    public PublicProfileResponse profile(@PathVariable Long userId) {
        return publicProfileService.profile(userId);
    }

    @GetMapping("/api/users/{userId:\\d+}/posts")
    public PublicPostFeedResponse posts(@PathVariable Long userId, @RequestParam(required = false) Integer page) {
        return publicProfileService.posts(userId, page);
    }
}
