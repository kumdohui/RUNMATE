package com.runmate.server.user.service;

import org.springframework.stereotype.Component;

import com.runmate.server.common.storage.S3ImageStorageService;
import com.runmate.server.user.domain.User;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProfileImageUrlResolver {

    private final S3ImageStorageService imageStorageService;

    public String resolve(User user) {
        return resolve(user, null);
    }

    public String resolve(User user, String socialProfileImageUrl) {
        if (user.getProfileImageStorageKey() != null
                && !user.getProfileImageStorageKey().isBlank()) {
            return imageStorageService.createViewUrl(user.getProfileImageStorageKey());
        }
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isBlank()) {
            return user.getProfileImageUrl();
        }
        return socialProfileImageUrl;
    }
}
