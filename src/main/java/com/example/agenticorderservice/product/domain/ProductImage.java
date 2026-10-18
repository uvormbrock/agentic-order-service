package com.example.agenticorderservice.product.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "product_images")
@Access(AccessType.FIELD)
public class ProductImage {
    @Id
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "original_key", nullable = false, columnDefinition = "text")
    private String originalKey;

    @Column(name = "thumbnail_key", nullable = false, columnDefinition = "text")
    private String thumbnailKey;

    @Column(name = "media_type", nullable = false, columnDefinition = "text")
    private String mediaType;

    @Column(name = "original_size", nullable = false)
    private long originalSize;

    @Column(name = "thumbnail_size", nullable = false)
    private long thumbnailSize;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(name = "thumbnail_width", nullable = false)
    private int thumbnailWidth;

    @Column(name = "thumbnail_height", nullable = false)
    private int thumbnailHeight;

    protected ProductImage() {}

    public ProductImage(
            UUID productId,
            String originalKey,
            String thumbnailKey,
            String mediaType,
            long originalSize,
            long thumbnailSize,
            int width,
            int height,
            int thumbnailWidth,
            int thumbnailHeight) {
        this.productId = java.util.Objects.requireNonNull(productId);
        replace(
                originalKey,
                thumbnailKey,
                mediaType,
                originalSize,
                thumbnailSize,
                width,
                height,
                thumbnailWidth,
                thumbnailHeight);
    }

    public void replace(
            String originalKey,
            String thumbnailKey,
            String mediaType,
            long originalSize,
            long thumbnailSize,
            int width,
            int height,
            int thumbnailWidth,
            int thumbnailHeight) {
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
        if (!originalKey.matches("[0-9a-f-]{36}/original")
                || !thumbnailKey.equals(originalKey.substring(0, 36) + "/thumbnail"))
            throw new IllegalArgumentException("Invalid image keys");
        this.originalKey = originalKey;
        this.thumbnailKey = thumbnailKey;
        this.mediaType = mediaType;
        this.originalSize = originalSize;
        this.thumbnailSize = thumbnailSize;
        this.width = width;
        this.height = height;
        this.thumbnailWidth = thumbnailWidth;
        this.thumbnailHeight = thumbnailHeight;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getOriginalKey() {
        return originalKey;
    }

    public String getThumbnailKey() {
        return thumbnailKey;
    }

    public String getMediaType() {
        return mediaType;
    }

    public long getOriginalSize() {
        return originalSize;
    }

    public long getThumbnailSize() {
        return thumbnailSize;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getThumbnailWidth() {
        return thumbnailWidth;
    }

    public int getThumbnailHeight() {
        return thumbnailHeight;
    }
}
