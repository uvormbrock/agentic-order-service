package com.example.agenticorderservice.product.application.port.out;

import com.example.agenticorderservice.product.domain.Product;
import java.util.Optional;
import java.util.UUID;

/** Product-only managed aggregate access within the owning application transaction. */
public interface ProductWriteStore {
    Product save(Product product);

    Optional<Product> findLockedById(UUID id);
}
