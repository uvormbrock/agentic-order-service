package com.example.agenticorderservice.product.application;

import com.example.agenticorderservice.product.application.port.in.GetProductQuery;
import com.example.agenticorderservice.product.application.port.out.FindProductById;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetProductService implements GetProductQuery {
    private final FindProductById findProductById;

    public GetProductService(FindProductById findProductById) {
        this.findProductById = findProductById;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResult get(UUID id) {
        return findProductById.findById(id).orElseThrow(ProductNotFoundException::new);
    }
}
