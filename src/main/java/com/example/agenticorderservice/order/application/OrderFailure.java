package com.example.agenticorderservice.order.application;

public final class OrderFailure extends RuntimeException {
    public enum Reason {
        REQUIRED,
        ITEMS,
        QUANTITY,
        CUSTOMER_INACTIVE,
        PRODUCT_INACTIVE,
        NOT_FOUND,
        UNAVAILABLE,
        TECHNICAL
    }

    private final Reason reason;

    public OrderFailure(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public OrderFailure(Reason reason, Throwable cause) {
        super(reason.name(), cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
