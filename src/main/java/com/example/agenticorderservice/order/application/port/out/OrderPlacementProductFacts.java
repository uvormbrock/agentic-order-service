package com.example.agenticorderservice.order.application.port.out;

import com.example.agenticorderservice.product.application.ProductResult;
import java.util.UUID;

public interface OrderPlacementProductFacts {
    ProductResult lock(UUID id);
}
