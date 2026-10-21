package com.example.agenticorderservice.product.application;

import com.example.agenticorderservice.product.application.port.in.ProductFacts;
import com.example.agenticorderservice.product.application.port.out.ProductCatalogReads;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Product-owned protected facts, never a managed entity crossing a feature boundary. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class ProductOrderAccess implements ProductFacts {
    private final ProductCatalogReads reads;

    public ProductOrderAccess(ProductCatalogReads reads) {
        this.reads = reads;
    }

    @Override
    public ProductResult lock(UUID id) {
        return reads.lock(id);
    }
}
