package com.example.agenticorderservice.order.integration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.customer.application.CustomerResult;
import com.example.agenticorderservice.customer.application.port.in.CustomerFacts;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderPlacementCustomerAdapterTest {
    @Test
    void forwardsLockToProviderAndPreservesFactAndFailureIdentity() {
        var provider = mock(CustomerFacts.class);
        var adapter = new OrderPlacementCustomerAdapter(provider);
        UUID id = UUID.randomUUID();
        var facts = new CustomerResult(id, "Current", "current@example.com", true);
        when(provider.lock(id)).thenReturn(facts);
        assertThat(adapter.lock(id)).isSameAs(facts);
        var failure = new IllegalStateException("provider failure");
        when(provider.lock(id)).thenThrow(failure);
        assertThatThrownBy(() -> adapter.lock(id)).isSameAs(failure);
        verify(provider, times(2)).lock(id);
        verifyNoMoreInteractions(provider);
    }
}
