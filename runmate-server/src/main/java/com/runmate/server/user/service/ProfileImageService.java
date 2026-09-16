package com.runmate.server.user.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.common.storage.S3ImageStorageService;
import com.runmate.server.common.storage.StoredImage;
import com.runmate.server.user.domain.User;
import com.runmate.server.user.dto.ProfileImageResponse;
import com.runmate.server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileImageService {

    private final UserRepository userRepository;
    private final S3ImageStorageService imageStorageService;

    @Transactional
    public ProfileImageResponse change(Long userId, MultipartFile file) {
        User user = findUser(userId);
        String previousObjectKey = user.getProfileImageStorageKey();
        StoredImage storedImage = imageStorageService.upload(file, "profiles/" + userId);

        try {
            user.changeProfileImage(
                    imageStorageService.createStorageUri(storedImage.objectKey()),
                    storedImage.objectKey(),
                    storedImage.contentType(),
                    storedImage.fileSize()
            );
            userRepository.saveAndFlush(user);
        } catch (RuntimeException exception) {
            try {
                imageStorageService.delete(storedImage.objectKey());
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }

        if (previousObjectKey != null && !previousObjectKey.equals(storedImage.objectKey())) {
            imageStorageService.delete(previousObjectKey);
        }

        return new ProfileImageResponse(imageStorageService.createViewUrl(storedImage.objectKey()));
    }

    @Transactional
    public void delete(Long userId) {
        User user = findUser(userId);
        String objectKey = user.getProfileImageStorageKey();
        user.removeProfileImage();
        userRepository.saveAndFlush(user);
        imageStorageService.delete(objectKey);
    }

    @Transactional(readOnly = true)
    public String getViewUrl(Long userId, String fallbackUrl) {
        User user = findUser(userId);
        if (user.getProfileImageStorageKey() != null
                && !user.getProfileImageStorageKey().isBlank()) {
            return imageStorageService.createViewUrl(user.getProfileImageStorageKey());
        }
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isBlank()) {
            return user.getProfileImageUrl();
        }
        return fallbackUrl;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
    }
}
