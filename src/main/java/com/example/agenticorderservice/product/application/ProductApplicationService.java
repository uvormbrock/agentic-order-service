package com.example.agenticorderservice.product.application;
import com.example.agenticorderservice.product.domain.Product;
import com.example.agenticorderservice.product.persistence.ProductRepository;
import com.example.agenticorderservice.shared.application.InputValidationException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductApplicationService {
    private final ProductRepository repository;
    public ProductApplicationService(ProductRepository repository) { this.repository = repository; }
    @Transactional
    public ProductResult create(String name, BigDecimal price) {
        String normalized = InputValidationException.name(name);
        var validated = validatePrice(price);
        return ProductResult.from(repository.save(new Product(normalized, validated)));
    }
    @Transactional(readOnly = true)
    public ProductResult get(UUID id) { return ProductResult.from(find(id)); }
    @Transactional(readOnly = true)
    public List<ProductResult> list() { return repository.findAll().stream().map(ProductResult::from).toList(); }
    @Transactional
    public ProductResult update(UUID id, String name, BigDecimal price) {
        String normalized = InputValidationException.name(name);
        var validated = validatePrice(price);
        var entity = find(id);
        entity.update(normalized, validated);
        return ProductResult.from(entity);
    }
    @Transactional
    public ProductResult deactivate(UUID id) {
        var entity = find(id);
        if (entity.isActive()) { entity.deactivate(); }
        return ProductResult.from(entity);
    }
    /** Shared by transport validation and direct application callers; no persistence access. */
    public static com.example.agenticorderservice.shared.domain.Money validatePrice(BigDecimal value) {
        if (value == null) { throw new InputValidationException(InputValidationException.FieldFailure.REQUIRED); }
        if (value.scale() > 2 || value.signum() < 0 || value.compareTo(new BigDecimal("999999.99")) > 0) {
            throw new InputValidationException(InputValidationException.FieldFailure.PRICE);
        }
        return new com.example.agenticorderservice.shared.domain.Money(value);
    }
    private Product find(UUID id) { return repository.findById(id).orElseThrow(ProductNotFoundException::new); }
}
