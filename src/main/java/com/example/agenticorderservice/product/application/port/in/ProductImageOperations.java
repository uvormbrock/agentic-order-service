package com.example.agenticorderservice.product.application.port.in;

import com.example.agenticorderservice.product.application.ProductImageContent;
import java.util.UUID;

public interface ProductImageOperations {
    void upload(UUID productId, byte[] bytes, String declaredType);

    ProductImageContent get(UUID productId, boolean thumbnail);

    void delete(UUID productId);

    int reconcileOrphans(boolean quiesced);
}
