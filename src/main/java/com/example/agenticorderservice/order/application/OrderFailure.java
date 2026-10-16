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
    private boolean diagnosticOwned;

    public OrderFailure(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public OrderFailure(Reason reason, Throwable cause) {
        super(reason.name(), cause);
        this.reason = reason;
    }

    void markDiagnosticOwned() {
        diagnosticOwned = true;
    }

    /** Indicates a coordinator-owned diagnostic attempt, not durable log delivery. */
    public boolean diagnosticOwned() {
        return diagnosticOwned;
    }

    public Reason reason() {
        return reason;
    }
}
