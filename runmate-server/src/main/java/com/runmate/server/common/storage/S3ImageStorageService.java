package com.runmate.server.common.storage;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
@Slf4j
public class S3ImageStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;
    private final Duration presignedUrlDuration;
    private final long maxImageSize;

    public S3ImageStorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.presigned-url-minutes}") long presignedUrlMinutes,
            @Value("${aws.s3.max-image-size}") long maxImageSize
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
        this.presignedUrlDuration = Duration.ofMinutes(presignedUrlMinutes);
        this.maxImageSize = maxImageSize;
    }

    public StoredImage upload(MultipartFile file, String directory) {
        return upload(file, directory, maxImageSize, "이미지는 5MB 이하만 업로드할 수 있습니다.");
    }

    public StoredImage upload(MultipartFile file, String directory, long sizeLimit, String sizeLimitMessage) {
        validate(file, directory, sizeLimit, sizeLimitMessage);

        String contentType = file.getContentType();
        String objectKey = normalizeDirectory(directory)
                + "/" + UUID.randomUUID() + EXTENSIONS.get(contentType);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .contentLength(file.getSize())
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return new StoredImage(objectKey, contentType, file.getSize());
        } catch (IOException exception) {
            log.error("Failed to read image before S3 upload", exception);
            throw new ImageStorageException("이미지 파일을 읽을 수 없습니다.", exception);
        } catch (RuntimeException exception) {
            log.error("S3 image upload failed: bucket={}, key={}, reason={}",
                    bucket, objectKey, exception.getMessage(), exception);
            throw new ImageStorageException("이미지 업로드에 실패했습니다.", exception);
        }
    }

    public String createViewUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(presignedUrlDuration)
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    public String createStorageUri(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        return "s3://" + bucket + "/" + objectKey;
    }

    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
        } catch (RuntimeException exception) {
            log.error("S3 image deletion failed: bucket={}, key={}, reason={}",
                    bucket, objectKey, exception.getMessage(), exception);
            throw new ImageStorageException("이미지 삭제에 실패했습니다.", exception);
        }
    }

    private void validate(MultipartFile file, String directory, long sizeLimit, String sizeLimitMessage) {
        if (bucket.isBlank()) {
            throw new ImageStorageException("S3 버킷 설정이 없습니다.");
        }
        if (file == null || file.isEmpty()) {
            throw new ImageStorageException("이미지를 선택해 주세요.");
        }
        if (file.getSize() > sizeLimit) {
            throw new ImageStorageException(sizeLimitMessage);
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new ImageStorageException("JPG, PNG, WEBP 이미지만 업로드할 수 있습니다.");
        }
        if (!hasMatchingSignature(file)) {
            throw new ImageStorageException("이미지 파일의 실제 형식이 올바르지 않습니다.");
        }
        if (directory == null || !directory.matches("[a-z0-9/_-]+")) {
            throw new ImageStorageException("이미지 저장 경로가 올바르지 않습니다.");
        }
    }

    private boolean hasMatchingSignature(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            return switch (file.getContentType()) {
                case "image/jpeg" -> header.length >= 3 && unsigned(header[0]) == 0xff
                        && unsigned(header[1]) == 0xd8 && unsigned(header[2]) == 0xff;
                case "image/png" -> header.length >= 8 && unsigned(header[0]) == 0x89
                        && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                        && unsigned(header[4]) == 0x0d && unsigned(header[5]) == 0x0a
                        && unsigned(header[6]) == 0x1a && unsigned(header[7]) == 0x0a;
                case "image/webp" -> header.length >= 12
                        && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                        && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
                default -> false;
            };
        } catch (IOException exception) {
            throw new ImageStorageException("이미지 파일을 읽을 수 없습니다.", exception);
        }
    }

    private int unsigned(byte value) {
        return value & 0xff;
    }

    private String normalizeDirectory(String directory) {
        return directory.replaceAll("^/+|/+$", "");
    }
}
