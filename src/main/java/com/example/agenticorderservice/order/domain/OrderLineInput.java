package com.example.agenticorderservice.order.domain;

import com.example.agenticorderservice.shared.domain.Money;
import java.util.Objects;
import java.util.UUID;

/** Trusted catalog facts for pure domain construction, never an HTTP request. */
public record OrderLineInput(UUID productId, String productName, Money unitPrice, int quantity) {
    public OrderLineInput {
        Objects.requireNonNull(productId, "productId");
        productName = Objects.requireNonNull(productName, "productName").strip();
        int length = productName.codePointCount(0, productName.length());
        if (productName.codePoints().anyMatch(c -> c >= 0xD800 && c <= 0xDFFF)
                || length < 1 || length > 200 || quantity < 1 || quantity > 10000) {
            throw new IllegalArgumentException("Invalid order line name or quantity");
        }
        Objects.requireNonNull(unitPrice, "unitPrice");
        if (unitPrice.compareTo(Money.zero()) < 0 || unitPrice.compareTo(Money.of("999999.99")) > 0) {
            throw new IllegalArgumentException("Invalid catalog unit price");
        }
    }
}
