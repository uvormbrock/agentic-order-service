package com.example.agenticorderservice.product.application.port.in;

import com.example.agenticorderservice.product.application.ProductResult;
import java.math.BigDecimal;
import java.util.UUID;

public interface ProductWriteOperations {
    ProductResult create(String name, BigDecimal price);

    ProductResult update(UUID id, String name, BigDecimal price);

    ProductResult deactivate(UUID id);
}
