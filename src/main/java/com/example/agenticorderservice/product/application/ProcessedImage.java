package com.example.agenticorderservice.product.application;

import java.util.Arrays;
import java.util.Objects;

public record ProcessedImage(
        byte[] original,
        byte[] thumbnail,
        String mediaType,
        int width,
        int height,
        int thumbnailWidth,
        int thumbnailHeight) {
    @Override
    public boolean equals(Object other) {
        return this == other
                || (other
                                instanceof
                                ProcessedImage(
                                        byte[] otherOriginal,
                                        byte[] otherThumbnail,
                                        String otherMediaType,
                                        int otherWidth,
                                        int otherHeight,
                                        int otherThumbnailWidth,
                                        int otherThumbnailHeight)
                        && Arrays.equals(original, otherOriginal)
                        && Arrays.equals(thumbnail, otherThumbnail)
                        && Objects.equals(mediaType, otherMediaType)
                        && width == otherWidth
                        && height == otherHeight
                        && thumbnailWidth == otherThumbnailWidth
                        && thumbnailHeight == otherThumbnailHeight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                Arrays.hashCode(original),
                Arrays.hashCode(thumbnail),
                mediaType,
                width,
                height,
                thumbnailWidth,
                thumbnailHeight);
    }

    @Override
    public String toString() {
        return "ProcessedImage[originalLength=" + (original == null ? "null" : original.length)
                + ", thumbnailLength=" + (thumbnail == null ? "null" : thumbnail.length)
                + ", mediaType=" + mediaType + ", width=" + width + ", height=" + height
                + ", thumbnailWidth=" + thumbnailWidth + ", thumbnailHeight=" + thumbnailHeight + "]";
    }
}
