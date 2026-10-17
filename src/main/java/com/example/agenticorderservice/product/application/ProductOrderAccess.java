package com.example.agenticorderservice.product.application;

import com.example.agenticorderservice.product.persistence.ProductRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Product-owned protected facts, never a managed entity crossing a feature boundary. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class ProductOrderAccess {
    private final ProductRepository repository;

    public ProductOrderAccess(ProductRepository repository) {
        this.repository = repository;
    }

    public ProductResult lock(UUID id) {
        return ProductResult.from(repository.findLockedById(id).orElseThrow(ProductNotFoundException::new));
    }
}
