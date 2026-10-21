package com.example.agenticorderservice.product.application.port.in;

import com.example.agenticorderservice.product.application.ProductResult;
import java.util.UUID;

/** Product-owned immutable locked facts participating in the caller's transaction. */
public interface ProductFacts {
    ProductResult lock(UUID id);
}
