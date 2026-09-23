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
        String uploaded = resolve(user.getProfileImageStorageKey(), user.getProfileImageUrl());
        return uploaded != null ? uploaded : socialProfileImageUrl;
    }

    public String resolve(String storageKey, String profileImageUrl) {
        if (storageKey != null && !storageKey.isBlank()) {
            return imageStorageService.createViewUrl(storageKey);
        }
        if (profileImageUrl != null && !profileImageUrl.isBlank()) {
            return profileImageUrl;
        }
        return null;
    }
}
