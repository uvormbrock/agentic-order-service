package com.example.agenticorderservice.product.application;

public final class ProductImageFailure extends RuntimeException {
    public enum Reason {
        INVALID,
        TOO_LARGE,
        UNSUPPORTED,
        NOT_FOUND,
        WRITES_DISABLED,
        STORAGE
    }

    private final Reason reason;

    public ProductImageFailure(Reason reason) {
        super("Product image operation failed: " + reason);
        this.reason = reason;
    }

    public ProductImageFailure(Reason reason, Throwable cause) {
        super("Product image operation failed: " + reason, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
