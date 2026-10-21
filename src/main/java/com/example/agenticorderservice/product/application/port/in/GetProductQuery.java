package com.example.agenticorderservice.product.application.port.in;

import com.example.agenticorderservice.product.application.ProductResult;
import java.util.UUID;

public interface GetProductQuery {
    ProductResult get(UUID id);
}
