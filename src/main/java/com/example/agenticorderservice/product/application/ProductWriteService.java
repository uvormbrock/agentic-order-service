package com.example.agenticorderservice.product.application;

import com.example.agenticorderservice.product.application.port.in.ProductWriteOperations;
import com.example.agenticorderservice.product.application.port.out.ProductWriteStore;
import com.example.agenticorderservice.product.domain.Product;
import com.example.agenticorderservice.shared.application.InputValidationException;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductWriteService implements ProductWriteOperations {
    private final ProductWriteStore store;

    public ProductWriteService(ProductWriteStore store) {
        this.store = store;
    }

    @Override
    @Transactional
    public ProductResult create(String name, BigDecimal price) {
        String normalized = InputValidationException.name(name);
        var validated = validatePrice(price);
        return ProductResult.from(store.save(new Product(normalized, validated)));
    }

    @Override
    @Transactional
    public ProductResult update(UUID id, String name, BigDecimal price) {
        String normalized = InputValidationException.name(name);
        var validated = validatePrice(price);
        var entity = findLocked(id);
        entity.update(normalized, validated);
        return ProductResult.from(entity);
    }

    @Override
    @Transactional
    public ProductResult deactivate(UUID id) {
        var entity = findLocked(id);
        if (entity.isActive()) {
            entity.deactivate();
        }
        return ProductResult.from(entity);
    }

    /** Shared by transport validation and direct application callers; no persistence access. */
    public static com.example.agenticorderservice.shared.domain.Money validatePrice(BigDecimal value) {
        if (value == null) {
            throw new InputValidationException(InputValidationException.FieldFailure.REQUIRED);
        }
        if (value.scale() > 2 || value.signum() < 0 || value.compareTo(new BigDecimal("999999.99")) > 0) {
            throw new InputValidationException(InputValidationException.FieldFailure.PRICE);
        }
        return new com.example.agenticorderservice.shared.domain.Money(value);
    }

    private Product findLocked(UUID id) {
        return store.findLockedById(id).orElseThrow(ProductNotFoundException::new);
    }
}
