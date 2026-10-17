package com.example.agenticorderservice.product.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record ProductDetailsRequest(
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description =
                        "After stripping surrounding Unicode whitespace: 1–200 Unicode code points. Malformed surrogate sequences are rejected.",
                example = "Demo Keyboard")
        String name,

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description =
                        "EUR gross price: 0–999999.99, ordinary JSON decimal spelling, at most two source decimal places. Exponents, strings and coercion are rejected.",
                minimum = "0",
                maximum = "999999.99",
                multipleOf = 0.01,
                example = "50.00")
        BigDecimal price) {}
