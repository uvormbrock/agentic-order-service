package com.example.agenticorderservice.customer.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record EmailAddress(String value) {
    private static final Pattern LOCAL = Pattern.compile("[a-zA-Z0-9!#$%&'*+/=?^_`{|}~.-]+");
    private static final Pattern LABEL = Pattern.compile("[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?");

    public EmailAddress {
        value = Objects.requireNonNull(value, "email").strip();
        if (value.chars().anyMatch(c -> c > 127)) {
            throw new IllegalArgumentException("Email must be ASCII");
        }
        value = value.toLowerCase(Locale.ROOT);
        int separator = value.indexOf('@');
        if (value.codePointCount(0, value.length()) > 254 || separator < 1 || separator != value.lastIndexOf('@')) {
            throw new IllegalArgumentException("Invalid email");
        }
        String local = value.substring(0, separator);
        String[] labels = value.substring(separator + 1).split("\\.", -1);
        if (!LOCAL.matcher(local).matches()
                || local.startsWith(".")
                || local.endsWith(".")
                || local.contains("..")
                || labels.length < 2) {
            throw new IllegalArgumentException("Invalid email");
        }
        for (String label : labels) {
            if (!LABEL.matcher(label).matches()) {
                throw new IllegalArgumentException("Invalid email domain");
            }
        }
    }
}
