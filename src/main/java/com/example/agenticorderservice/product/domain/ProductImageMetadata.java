package com.example.agenticorderservice.product.domain;

public record ProductImageMetadata(
        String mediaType,
        long originalSize,
        long thumbnailSize,
        int width,
        int height,
        int thumbnailWidth,
        int thumbnailHeight) {
    public ProductImageMetadata {
        if (!java.util.Set.of("image/jpeg", "image/png").contains(mediaType)
                || originalSize <= 0
                || originalSize > 10 * 1024 * 1024
                || thumbnailSize <= 0
                || width < 1
                || height < 1
                || width > 16384
                || height > 16384
                || (long) width * height > 40_000_000L
                || thumbnailWidth < 1
                || thumbnailHeight < 1
                || thumbnailWidth > 320
                || thumbnailHeight > 320
                || thumbnailWidth > width
                || thumbnailHeight > height) throw new IllegalArgumentException("Invalid image metadata");
    }
}
