package com.runmate.server.common.storage;

public record StoredImage(String objectKey, String contentType, long fileSize) {
}
