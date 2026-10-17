package com.example.agenticorderservice.product.api;

import com.example.agenticorderservice.product.application.ProductResult;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(UUID id, String name, BigDecimal price, boolean active) {
    static ProductResponse from(ProductResult result) {
        return new ProductResponse(result.id(), result.name(), result.price(), result.active());
    }
}
