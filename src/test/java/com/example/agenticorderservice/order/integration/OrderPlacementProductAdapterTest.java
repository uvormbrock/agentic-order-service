package com.example.agenticorderservice.order.integration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.product.application.ProductResult;
import com.example.agenticorderservice.product.application.port.in.ProductFacts;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderPlacementProductAdapterTest {
    @Test
    void forwardsLockToProviderAndPreservesFactAndFailureIdentity() {
        var provider = mock(ProductFacts.class);
        var adapter = new OrderPlacementProductAdapter(provider);
        UUID id = UUID.randomUUID();
        var facts = new ProductResult(id, "Captured", new java.math.BigDecimal("25.00"), true);
        when(provider.lock(id)).thenReturn(facts);
        assertThat(adapter.lock(id)).isSameAs(facts);
        var failure = new IllegalStateException("provider failure");
        when(provider.lock(id)).thenThrow(failure);
        assertThatThrownBy(() -> adapter.lock(id)).isSameAs(failure);
        verify(provider, times(2)).lock(id);
        verifyNoMoreInteractions(provider);
    }
}
