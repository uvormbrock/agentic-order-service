package com.example.agenticorderservice.product.application;

public final class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException() {
        super("Product not found");
    }
}
