package com.example.agenticorderservice.product.application.port.out;

import java.util.function.Supplier;

/** Synchronous execution returns only after transaction completion; exceptions retain their original provenance. */
public interface ProductImageTransactions {
    void requireNoAmbientTransaction();

    <T> T execute(Supplier<T> operation);
}
