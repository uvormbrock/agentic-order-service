package com.example.agenticorderservice.product.application.port.out;

import com.example.agenticorderservice.product.application.ProductResult;
import java.util.Optional;
import java.util.UUID;

public interface FindProductById {
    Optional<ProductResult> findById(UUID id);
}
