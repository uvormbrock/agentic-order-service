package com.example.agenticorderservice.product.persistence;

import com.example.agenticorderservice.product.domain.Product;
import org.springframework.data.repository.Repository;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends Repository<Product, UUID> {
    Product save(Product entity);
    Optional<Product> findById(UUID id);
}
