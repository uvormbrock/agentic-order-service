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
    private final boolean diagnosticOwned;

    public OrderFailure(Reason reason) {
        super(reason.name());
        this.reason = reason;
        this.diagnosticOwned = false;
    }

    public OrderFailure(Reason reason, Throwable cause) {
        this(reason, cause, false);
    }

    OrderFailure(Reason reason, Throwable cause, boolean diagnosticOwned) {
        super(reason.name(), cause);
        this.reason = reason;
        this.diagnosticOwned = diagnosticOwned;
    }

    OrderFailure withDiagnosticOwnership() {
        if (diagnosticOwned) return this;
        var diagnosed = new OrderFailure(reason, getCause(), true);
        diagnosed.setStackTrace(getStackTrace());
        for (Throwable suppressed : getSuppressed()) diagnosed.addSuppressed(suppressed);
        return diagnosed;
    }

    /** Indicates a coordinator-owned diagnostic attempt, not durable log delivery. */
    public boolean diagnosticOwned() {
        return diagnosticOwned;
    }

    public Reason reason() {
        return reason;
    }
}
