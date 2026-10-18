package com.example.agenticorderservice.product.application;

public record ProductImageContent(byte[] bytes, String mediaType) {
    public ProductImageContent {
        bytes = bytes.clone();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
