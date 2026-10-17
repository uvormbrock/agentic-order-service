package com.example.agenticorderservice.order.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record OrderItemRequest(
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description = "Existing active product, referenced by canonical UUID text.",
                format = "uuid",
                example = "11111111-2222-3333-4444-555555555555")
        UUID productId,

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description =
                        "Positive JSON integer token (not 1.0, exponents, strings or booleans). Consolidated quantity per product must not exceed 10,000.",
                minimum = "1",
                maximum = "10000",
                example = "2")
        Integer quantity) {}
