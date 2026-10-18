package com.example.agenticorderservice.product.persistence;

import com.example.agenticorderservice.product.domain.ProductImage;
import java.util.*;
import org.springframework.data.repository.Repository;

public interface ProductImageRepository extends Repository<ProductImage, UUID> {
    Optional<ProductImage> findById(UUID productId);

    ProductImage save(ProductImage image);

    void delete(ProductImage image);

    List<ProductImage> findAll();
}
