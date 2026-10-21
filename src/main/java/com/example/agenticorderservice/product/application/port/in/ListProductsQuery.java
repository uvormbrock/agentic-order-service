package com.example.agenticorderservice.product.application.port.in;

import com.example.agenticorderservice.product.application.ProductResult;
import java.util.List;

public interface ListProductsQuery {
    List<ProductResult> list();
}
