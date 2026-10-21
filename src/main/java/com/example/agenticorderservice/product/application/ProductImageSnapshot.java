package com.example.agenticorderservice.product.application;

import java.util.UUID;

public record ProductImageSnapshot(
        UUID productId,
        String originalKey,
        String thumbnailKey,
        String mediaType,
        long originalSize,
        long thumbnailSize,
        int width,
        int height,
        int thumbnailWidth,
        int thumbnailHeight) {}
