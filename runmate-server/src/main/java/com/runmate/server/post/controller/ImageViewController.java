package com.runmate.server.post.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.runmate.server.post.dto.ImageViewUrlResponse;
import com.runmate.server.post.service.PostImageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageViewController {
    private final PostImageService postImageService;

    @GetMapping("/{imageId}/view-url")
    public ImageViewUrlResponse viewUrl(@PathVariable Long imageId) {
        return postImageService.createViewUrl(imageId);
    }
}
