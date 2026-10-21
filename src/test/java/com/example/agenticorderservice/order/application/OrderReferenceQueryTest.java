package com.example.agenticorderservice.order.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.order.application.port.out.OrderReferenceStore;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderReferenceQueryTest {
    @Test
    void referenceCapabilityPreservesTrueFalseAndOriginalStoreFailure() {
        var store = mock(OrderReferenceStore.class);
        var provider = new OrderReferenceQuery(store);
        UUID id = UUID.randomUUID();
        var failure = new IllegalStateException("provider failure");
        when(store.references(id)).thenReturn(true, false).thenThrow(failure);
        assertThat(provider.references(id)).isTrue();
        assertThat(provider.references(id)).isFalse();
        assertThatThrownBy(() -> provider.references(id)).isSameAs(failure);
        verify(store, times(3)).references(id);
        verifyNoMoreInteractions(store);
    }
}
