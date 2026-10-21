package com.example.agenticorderservice.product.persistence;

import com.example.agenticorderservice.product.application.ProductNotFoundException;
import com.example.agenticorderservice.product.application.ProductResult;
import com.example.agenticorderservice.product.application.port.out.FindProductById;
import com.example.agenticorderservice.product.application.port.out.ProductCatalogReads;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProductReadAdapter implements FindProductById, ProductCatalogReads {
    private final ProductRepository repository;

    public ProductReadAdapter(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ProductResult> findById(UUID id) {
        return repository
                .findById(id)
                .map(product -> new ProductResult(
                        product.getId(), product.getName(), product.getPrice().amount(), product.isActive()));
    }

    @Override
    public List<ProductResult> findAll() {
        return repository.findAll().stream()
                .map(product -> new ProductResult(
                        product.getId(), product.getName(), product.getPrice().amount(), product.isActive()))
                .toList();
    }

    @Override
    public ProductResult lock(UUID id) {
        var product = repository.findLockedById(id).orElseThrow(ProductNotFoundException::new);
        return new ProductResult(
                product.getId(), product.getName(), product.getPrice().amount(), product.isActive());
    }
}
