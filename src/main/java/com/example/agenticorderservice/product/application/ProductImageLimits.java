package com.example.agenticorderservice.product.application;

/** Existing shared bounds for transport reads, processing and managed file reads. */
public final class ProductImageLimits {
    public static final int MAX_BYTES = 10 * 1024 * 1024;
    public static final long MAX_PIXELS = 40_000_000L;
    public static final int MAX_EDGE = 16_384;
    public static final int THUMBNAIL_EDGE = 320;

    private ProductImageLimits() {}
}
