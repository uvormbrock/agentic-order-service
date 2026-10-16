package com.example.agenticorderservice.product.application;

import com.example.agenticorderservice.product.domain.Product;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductResult(UUID id, String name, BigDecimal price, boolean active) {
    static ProductResult from(Product entity) {
        return new ProductResult(
                entity.getId(), entity.getName(), entity.getPrice().amount(), entity.isActive());
    }
}
