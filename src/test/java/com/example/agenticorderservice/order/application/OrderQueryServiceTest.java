package com.example.agenticorderservice.order.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.customer.application.*;
import com.example.agenticorderservice.order.domain.*;
import com.example.agenticorderservice.order.persistence.OrderRepository;
import com.example.agenticorderservice.shared.domain.Money;
import java.util.*;
import org.junit.jupiter.api.Test;

class OrderQueryServiceTest {
    @Test
    void batchesDistinctCurrentCustomersAndReturnsImmutableHistoricalFacts() {
        var orders = mock(OrderRepository.class);
        var customers = mock(CustomerOrderAccess.class);
        var service = new OrderQueryService(orders, customers);
        UUID customer = UUID.randomUUID(), product = UUID.randomUUID();
        var order = new Order(customer, List.of(new OrderLineInput(product, "Original", Money.of("100"), 1)));
        when(orders.findAllWithItems()).thenReturn(List.of(order, order));
        when(customers.current(Set.of(customer)))
                .thenReturn(Map.of(customer, new CustomerResult(customer, "Current", "a@example.com", false)));
        var results = service.list();
        assertThat(results).hasSize(2);
        assertThat(results.getFirst().customer().active()).isFalse();
        assertThat(results.getFirst().items().getFirst().productName()).isEqualTo("Original");
        verify(customers).current(Set.of(customer));
        assertThatThrownBy(results::clear).isInstanceOf(UnsupportedOperationException.class);
        when(orders.findByIdWithItems(product)).thenReturn(Optional.of(order));
        var detail = service.get(product);
        assertThat(detail.customer()).isEqualTo(results.getFirst().customer());
        assertThat(detail.items()).isEqualTo(results.getFirst().items());
        assertThat(detail.total()).isEqualByComparingTo("90");
        when(orders.findAllWithItems()).thenReturn(List.of());
        assertThat(service.list()).isEmpty();
        UUID missingId = UUID.randomUUID();
        assertThatExceptionOfType(OrderFailure.class)
                .isThrownBy(() -> service.get(missingId))
                .satisfies(f -> assertThat(f.reason()).isEqualTo(OrderFailure.Reason.NOT_FOUND));
    }
}
