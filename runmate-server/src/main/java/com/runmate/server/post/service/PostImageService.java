package com.runmate.server.post.service;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.runmate.server.auth.exception.AuthException;
import com.runmate.server.common.storage.ImageStorageException;
import com.runmate.server.common.storage.S3ImageStorageService;
import com.runmate.server.common.storage.StoredImage;
import com.runmate.server.post.domain.Post;
import com.runmate.server.post.domain.PostImage;
import com.runmate.server.post.dto.ImageViewUrlResponse;
import com.runmate.server.post.dto.PostImageResponse;
import com.runmate.server.post.mapper.PostImageMapper;
import com.runmate.server.post.mapper.PostMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostImageService {
    private static final int MAX_IMAGES_PER_POST = 10;

    private final PostMapper postMapper;
    private final PostImageMapper postImageMapper;
    private final S3ImageStorageService storageService;

    @Value("${aws.s3.max-post-image-size}")
    private long maxPostImageSize;

    @Value("${aws.s3.presigned-url-minutes}")
    private long presignedUrlMinutes;

    @Transactional
    public List<PostImageResponse> upload(Long userId, Long postId,
            List<MultipartFile> files, List<Integer> displayOrders) {
        requireOwner(userId, postId);
        if (files == null || files.isEmpty()) {
            throw new ImageStorageException("이미지를 한 장 이상 선택해 주세요.");
        }
        long existingCount = postImageMapper.countByPostId(postId);
        if (existingCount + files.size() > MAX_IMAGES_PER_POST) {
            throw new ImageStorageException("게시글 이미지는 최대 10장까지 등록할 수 있습니다.");
        }
        if (displayOrders != null && !displayOrders.isEmpty()
                && displayOrders.size() != files.size()) {
            throw new ImageStorageException("이미지 수와 표시 순서 수가 일치하지 않습니다.");
        }

        List<StoredImage> uploaded = new ArrayList<>();
        List<PostImage> entities = new ArrayList<>();
        try {
            for (int index = 0; index < files.size(); index++) {
                MultipartFile file = files.get(index);
                StoredImage stored = storageService.upload(file, "posts/" + postId,
                        maxPostImageSize, "게시글 이미지는 파일당 10MB 이하만 업로드할 수 있습니다.");
                uploaded.add(stored);
                int order = displayOrders == null || displayOrders.isEmpty()
                        ? Math.toIntExact(existingCount + index)
                        : displayOrders.get(index);
                if (order < 0) {
                    throw new ImageStorageException("이미지 표시 순서는 0 이상이어야 합니다.");
                }
                entities.add(PostImage.create(postId, originalName(file), stored.objectKey(),
                        stored.contentType(),
                        stored.fileSize(), order));
            }
            List<PostImage> saved = postImageMapper.saveAllAndFlush(entities);
            return saved.stream().map(this::toResponse).toList();
        } catch (RuntimeException exception) {
            uploaded.forEach(image -> deleteQuietly(image.objectKey()));
            if (exception instanceof ImageStorageException storageException) {
                throw storageException;
            }
            throw new ImageStorageException("게시글 이미지 정보를 저장하지 못했습니다.", exception);
        }
    }

    @Transactional
    public void delete(Long userId, Long postId, Long imageId) {
        requireOwner(userId, postId);
        PostImage image = postImageMapper.findByImageIdAndPostId(imageId, postId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "게시글 이미지를 찾을 수 없습니다."));
        postImageMapper.delete(image);
        postImageMapper.flush();
        storageService.delete(image.getStorageKey());
    }

    @Transactional(readOnly = true)
    public ImageViewUrlResponse createViewUrl(Long imageId) {
        PostImage image = postImageMapper.findById(imageId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "게시글 이미지를 찾을 수 없습니다."));
        return new ImageViewUrlResponse(storageService.createViewUrl(image.getStorageKey()),
                Instant.now().plus(Duration.ofMinutes(presignedUrlMinutes)));
    }

    private void requireOwner(Long userId, Long postId) {
        Post post = postMapper.findById(postId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        if (!post.getAuthorId().equals(userId)) {
            throw new AuthException(HttpStatus.FORBIDDEN, "게시글 작성자만 이미지를 관리할 수 있습니다.");
        }
    }

    private PostImageResponse toResponse(PostImage image) {
        return new PostImageResponse(image.getImageId(), image.getOriginalName(),
                image.getContentType(), image.getFileSize(), image.getDisplayOrder(),
                storageService.createViewUrl(image.getStorageKey()));
    }

    private String originalName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) return "image";
        String safeName = Path.of(name).getFileName().toString();
        return safeName.length() <= 255 ? safeName : safeName.substring(safeName.length() - 255);
    }

    private void deleteQuietly(String objectKey) {
        try {
            storageService.delete(objectKey);
        } catch (RuntimeException ignored) {
            // The original failure is returned; orphan cleanup can retry this object later.
        }
    }
}


