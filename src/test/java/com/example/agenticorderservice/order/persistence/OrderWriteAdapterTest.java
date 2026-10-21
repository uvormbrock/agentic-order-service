package com.example.agenticorderservice.order.persistence;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.order.domain.Order;
import com.example.agenticorderservice.order.domain.OrderLineInput;
import com.example.agenticorderservice.shared.domain.Money;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderWriteAdapterTest {
    @Test
    void delegatesSameAggregateSaveAndFlushWithoutReconstruction() {
        var repository = mock(OrderRepository.class);
        var adapter = new OrderWriteAdapter(repository);
        var aggregate = new Order(
                UUID.randomUUID(), List.of(new OrderLineInput(UUID.randomUUID(), "Captured", Money.of("100.05"), 1)));
        var originalItem = aggregate.getItems().getFirst();
        when(repository.save(aggregate)).thenReturn(aggregate);
        assertThat(adapter.save(aggregate)).isSameAs(aggregate);
        adapter.flush();
        assertThat(aggregate.getItems()).containsExactly(originalItem);
        assertThat(originalItem.getOrder()).isSameAs(aggregate);
        assertThat(aggregate.getTotal()).isEqualTo(Money.of("90.04"));
        var sequence = inOrder(repository);
        sequence.verify(repository).save(aggregate);
        sequence.verify(repository).flush();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void preservesOriginalSaveAndFlushFailures() {
        var repository = mock(OrderRepository.class);
        var adapter = new OrderWriteAdapter(repository);
        var aggregate = new Order(
                UUID.randomUUID(), List.of(new OrderLineInput(UUID.randomUUID(), "Captured", Money.of("1"), 1)));
        var saveFailure = new IllegalStateException("save failure");
        when(repository.save(aggregate)).thenThrow(saveFailure);
        assertThatThrownBy(() -> adapter.save(aggregate)).isSameAs(saveFailure);
        verify(repository, never()).flush();
        var flushFailure = new IllegalStateException("flush failure");
        doThrow(flushFailure).when(repository).flush();
        assertThatThrownBy(adapter::flush).isSameAs(flushFailure);
    }
}
