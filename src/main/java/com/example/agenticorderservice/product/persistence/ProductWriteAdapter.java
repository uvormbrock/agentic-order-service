package com.example.agenticorderservice.product.persistence;

import com.example.agenticorderservice.product.application.port.out.ProductWriteStore;
import com.example.agenticorderservice.product.domain.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProductWriteAdapter implements ProductWriteStore {
    private final ProductRepository repository;

    public ProductWriteAdapter(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public Product save(Product product) {
        return repository.save(product);
    }

    @Override
    public Optional<Product> findLockedById(UUID id) {
        return repository.findLockedById(id);
    }
}
