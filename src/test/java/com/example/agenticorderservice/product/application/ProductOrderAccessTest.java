package com.example.agenticorderservice.product.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.product.application.port.out.ProductCatalogReads;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductOrderAccessTest {
    @Test
    void protectedFactsDelegateWithoutChangingIdentityActivityOrFailure() {
        var reads = mock(ProductCatalogReads.class);
        var provider = new ProductOrderAccess(reads);
        UUID id = UUID.randomUUID();
        var result = new ProductResult(id, "Inactive", new BigDecimal("12.34"), false);
        var failure = new ProductNotFoundException();
        when(reads.lock(id)).thenReturn(result).thenThrow(failure);
        assertThat(provider.lock(id)).isSameAs(result);
        assertThatThrownBy(() -> provider.lock(id)).isSameAs(failure);
        verify(reads, times(2)).lock(id);
        verifyNoMoreInteractions(reads);
    }
}
