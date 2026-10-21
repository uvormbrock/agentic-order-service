package com.example.agenticorderservice.order.integration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.customer.application.CustomerResult;
import com.example.agenticorderservice.customer.application.port.in.CustomerFacts;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderReadCustomerAdapterTest {
    @Test
    void forwardsCurrentBatchAndEmptyBatchToProviderWithoutChangingFacts() {
        var provider = mock(CustomerFacts.class);
        var adapter = new OrderReadCustomerAdapter(provider);
        UUID id = UUID.randomUUID();
        var facts = Map.of(id, new CustomerResult(id, "Current", "current@example.com", false));
        when(provider.current(Set.of(id))).thenReturn(facts);
        when(provider.current(Set.of())).thenReturn(Map.of());
        assertThat(adapter.current(Set.of(id))).isEqualTo(facts);
        assertThat(adapter.current(Set.of())).isEmpty();
        verify(provider).current(Set.of(id));
        verify(provider).current(Set.of());
        verifyNoMoreInteractions(provider);
    }
}
