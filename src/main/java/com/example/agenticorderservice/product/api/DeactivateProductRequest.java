package com.example.agenticorderservice.product.api;

import io.swagger.v3.oas.annotations.media.Schema;

public record DeactivateProductRequest(
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description = "Only JSON false is accepted; true cannot reactivate a product.",
                example = "false")
        Boolean active) {}
