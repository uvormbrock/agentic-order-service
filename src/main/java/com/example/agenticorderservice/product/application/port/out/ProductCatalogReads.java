package com.example.agenticorderservice.product.application.port.out;

import com.example.agenticorderservice.product.application.ProductResult;
import java.util.List;
import java.util.UUID;

/** Immutable catalog reads within the owning application's transaction. */
public interface ProductCatalogReads {
    List<ProductResult> findAll();

    ProductResult lock(UUID id);
}
