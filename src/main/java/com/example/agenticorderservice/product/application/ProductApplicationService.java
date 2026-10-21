package com.example.agenticorderservice.product.application;

import com.example.agenticorderservice.product.application.port.in.GetProductQuery;
import com.example.agenticorderservice.product.application.port.in.ListProductsQuery;
import com.example.agenticorderservice.product.application.port.in.ProductWriteOperations;
import com.example.agenticorderservice.product.application.port.out.ProductCatalogReads;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductApplicationService implements ListProductsQuery {
    private final ProductCatalogReads reads;
    private final GetProductQuery getProductQuery;
    private final ProductWriteOperations productWriteOperations;

    public ProductApplicationService(
            ProductCatalogReads reads, GetProductQuery getProductQuery, ProductWriteOperations productWriteOperations) {
        this.reads = reads;
        this.getProductQuery = getProductQuery;
        this.productWriteOperations = productWriteOperations;
    }

    @Transactional
    public ProductResult create(String name, BigDecimal price) {
        return productWriteOperations.create(name, price);
    }

    @Transactional(readOnly = true)
    public ProductResult get(UUID id) {
        return getProductQuery.get(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResult> list() {
        return reads.findAll();
    }

    @Transactional
    public ProductResult update(UUID id, String name, BigDecimal price) {
        return productWriteOperations.update(id, name, price);
    }

    @Transactional
    public ProductResult deactivate(UUID id) {
        return productWriteOperations.deactivate(id);
    }
    /** Shared by transport validation and direct application callers; no persistence access. */
    public static com.example.agenticorderservice.shared.domain.Money validatePrice(BigDecimal value) {
        return ProductWriteService.validatePrice(value);
    }
}
