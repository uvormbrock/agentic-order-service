package com.example.agenticorderservice.order.integration;

import com.example.agenticorderservice.order.application.port.out.OrderPlacementProductFacts;
import com.example.agenticorderservice.product.application.ProductResult;
import com.example.agenticorderservice.product.application.port.in.ProductFacts;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrderPlacementProductAdapter implements OrderPlacementProductFacts {
    private final ProductFacts products;

    public OrderPlacementProductAdapter(ProductFacts products) {
        this.products = products;
    }

    @Override
    public ProductResult lock(UUID id) {
        return products.lock(id);
    }
}
