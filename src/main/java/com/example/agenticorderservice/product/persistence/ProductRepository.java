package com.example.agenticorderservice.product.persistence;

import com.example.agenticorderservice.product.domain.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface ProductRepository extends Repository<Product, UUID> {
    java.util.List<Product> findAll();

    Product save(Product entity);

    Optional<Product> findById(UUID id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Product e where e.id = :id")
    Optional<Product> findLockedById(@org.springframework.data.repository.query.Param("id") UUID id);
}
