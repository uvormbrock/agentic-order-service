package com.example.agenticorderservice.customer.api;

import io.swagger.v3.oas.annotations.media.Schema;

public record CustomerDetailsRequest(
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description =
                        "After stripping surrounding Unicode whitespace: 1–200 Unicode code points. Malformed surrogate sequences are rejected.",
                example = "Demo Alice")
        String name,

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description =
                        "ASCII email, stripped and lowercased; normalized length at most 254. Unique including inactive customers.",
                example = "alice@example.invalid")
        String email) {}
