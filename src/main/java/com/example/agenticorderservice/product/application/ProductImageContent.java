package com.example.agenticorderservice.product.application;

import java.util.Arrays;
import java.util.Objects;

public record ProductImageContent(byte[] bytes, String mediaType) {
    public ProductImageContent {
        bytes = bytes.clone();
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof ProductImageContent(byte[] otherBytes, String otherMediaType)
                        && Arrays.equals(bytes, otherBytes)
                        && Objects.equals(mediaType, otherMediaType));
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(bytes) + Objects.hashCode(mediaType);
    }

    @Override
    public String toString() {
        return "ProductImageContent[byteLength=" + bytes.length + ", mediaType=" + mediaType + "]";
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
