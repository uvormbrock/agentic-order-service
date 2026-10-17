package com.example.agenticorderservice.customer.api;

import io.swagger.v3.oas.annotations.media.Schema;

public record DeactivateCustomerRequest(
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description = "Only JSON false is accepted; true cannot reactivate a customer.",
                example = "false")
        Boolean active) {}
