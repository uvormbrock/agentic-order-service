package com.example.agenticorderservice.product.application.port.out;

import com.example.agenticorderservice.product.application.ProductImageSnapshot;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ProductImageMetadataStore {
    void requireProductLocked(UUID productId);

    Optional<ProductImageSnapshot> findByProductId(UUID productId);

    void save(ProductImageSnapshot image);

    void delete(UUID productId);

    Set<String> referencedOriginalKeys();
}
