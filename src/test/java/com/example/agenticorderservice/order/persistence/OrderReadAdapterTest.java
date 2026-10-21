package com.example.agenticorderservice.order.persistence;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.order.domain.Order;
import com.example.agenticorderservice.order.domain.OrderLineInput;
import com.example.agenticorderservice.shared.domain.Money;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderReadAdapterTest {
    @Test
    void mapsAllHistoricalFactsAndItemsWithoutQueryingOtherResources() {
        var repository = mock(OrderRepository.class);
        var adapter = new OrderReadAdapter(repository);
        UUID id = UUID.randomUUID(), customer = UUID.randomUUID(), product = UUID.randomUUID();
        var root = new Order(customer, List.of(new OrderLineInput(product, "Captured", Money.of("100.05"), 1)));
        when(repository.findByIdWithItems(id)).thenReturn(Optional.of(root));
        var value = adapter.findById(id).orElseThrow();
        assertThat(value.id()).isEqualTo(root.getId());
        assertThat(value.customerId()).isEqualTo(customer);
        assertThat(value.currency()).isEqualTo("EUR");
        assertThat(value.subtotal()).isEqualByComparingTo("100.05");
        assertThat(value.discount()).isEqualByComparingTo("10.01");
        assertThat(value.total()).isEqualByComparingTo("90.04");
        assertThat(value.items()).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(product);
            assertThat(item.productName()).isEqualTo("Captured");
            assertThat(item.quantity()).isEqualTo(1);
            assertThat(item.unitPrice()).isEqualByComparingTo("100.05");
            assertThat(item.lineTotal()).isEqualByComparingTo("100.05");
        });
        assertThatThrownBy(value.items()::clear).isInstanceOf(UnsupportedOperationException.class);
        verify(repository).findByIdWithItems(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void preservesMissingAndEmptyResultsAndMaterializesEveryReturnedRoot() {
        var repository = mock(OrderRepository.class);
        var adapter = new OrderReadAdapter(repository);
        UUID id = UUID.randomUUID();
        when(repository.findByIdWithItems(id)).thenReturn(Optional.empty());
        assertThat(adapter.findById(id)).isEmpty();
        when(repository.findAllWithItems()).thenReturn(List.of());
        assertThat(adapter.findAll()).isEmpty();
        var first =
                new Order(UUID.randomUUID(), List.of(new OrderLineInput(UUID.randomUUID(), "First", Money.of("2"), 2)));
        var second = new Order(
                UUID.randomUUID(), List.of(new OrderLineInput(UUID.randomUUID(), "Second", Money.of("3"), 3)));
        when(repository.findAllWithItems()).thenReturn(List.of(first, second));
        var all = adapter.findAll();
        assertThat(all).hasSize(2);
        assertThat(all.get(0).items().getFirst().productName()).isEqualTo("First");
        assertThat(all.get(1).items().getFirst().productName()).isEqualTo("Second");
        assertThatThrownBy(all::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void referencesPreserveTrueFalseAndOriginalPersistenceFailure() {
        var repository = mock(OrderRepository.class);
        var adapter = new OrderReadAdapter(repository);
        UUID id = UUID.randomUUID();
        var failure = new IllegalStateException("provider failure");
        when(repository.existsByCustomerId(id)).thenReturn(true, false).thenThrow(failure);
        assertThat(adapter.references(id)).isTrue();
        assertThat(adapter.references(id)).isFalse();
        assertThatThrownBy(() -> adapter.references(id)).isSameAs(failure);
        verify(repository, times(3)).existsByCustomerId(id);
        verifyNoMoreInteractions(repository);
    }
}
