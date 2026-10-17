package com.example.agenticorderservice.order.api;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description = "Existing active customer, referenced by canonical UUID text.",
                format = "uuid",
                example = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
        UUID customerId,

        @ArraySchema(
                minItems = 1,
                arraySchema =
                        @Schema(
                                requiredMode = Schema.RequiredMode.REQUIRED,
                                description =
                                        "Nonempty items; no null elements. Duplicate product IDs are consolidated."),
                schema = @Schema(implementation = OrderItemRequest.class))
        List<OrderItemRequest> items) {
    public CreateOrderRequest {
        items = List.copyOf(items);
    }
}
