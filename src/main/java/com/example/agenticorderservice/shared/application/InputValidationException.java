package com.example.agenticorderservice.shared.application;

/** Contextual input failures, independent of HTTP; domain invariants remain authoritative. */
public final class InputValidationException extends RuntimeException {
    public enum FieldFailure {
        REQUIRED,
        NAME,
        EMAIL,
        PRICE
    }

    private final FieldFailure failure;

    public InputValidationException(FieldFailure failure) {
        super("Invalid application input: " + failure);
        this.failure = failure;
    }

    public FieldFailure failure() {
        return failure;
    }

    public static String name(String value) {
        if (value == null) {
            throw new InputValidationException(FieldFailure.REQUIRED);
        }
        String normalized = value.strip();
        int length = normalized.codePointCount(0, normalized.length());
        if (length < 1 || length > 200 || normalized.codePoints().anyMatch(c -> c >= 0xD800 && c <= 0xDFFF)) {
            throw new InputValidationException(FieldFailure.NAME);
        }
        return normalized;
    }
}
