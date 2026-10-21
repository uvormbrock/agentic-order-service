package com.example.agenticorderservice.order.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.customer.application.*;
import com.example.agenticorderservice.order.application.port.out.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class OrderQueryServiceTest {
    @Test
    void batchesDistinctCurrentCustomersAndReturnsImmutableHistoricalFacts() {
        var orders = mock(OrderReadStore.class);
        var customers = mock(OrderCustomerFacts.class);
        var service = new OrderQueryService(orders, customers);
        UUID customer = UUID.randomUUID(), product = UUID.randomUUID();
        var order = snapshot(customer, product, "Original", "100", "90");
        when(orders.findAll()).thenReturn(List.of(order, order));
        when(customers.current(Set.of(customer)))
                .thenReturn(Map.of(customer, new CustomerResult(customer, "Current", "a@example.com", false)));
        var results = service.list();
        assertThat(results).hasSize(2);
        assertThat(results.getFirst().customer().active()).isFalse();
        assertThat(results.getFirst().items().getFirst().productName()).isEqualTo("Original");
        verify(customers).current(Set.of(customer));
        assertThatThrownBy(results::clear).isInstanceOf(UnsupportedOperationException.class);
        when(orders.findById(product)).thenReturn(Optional.of(order));
        var detail = service.get(product);
        assertThat(detail.customer()).isEqualTo(results.getFirst().customer());
        assertThat(detail.items()).isEqualTo(results.getFirst().items());
        assertThat(detail.total()).isEqualByComparingTo("90");
        when(orders.findAll()).thenReturn(List.of());
        assertThat(service.list()).isEmpty();
        UUID missingId = UUID.randomUUID();
        assertThatExceptionOfType(OrderFailure.class)
                .isThrownBy(() -> service.get(missingId))
                .satisfies(f -> assertThat(f.reason()).isEqualTo(OrderFailure.Reason.NOT_FOUND));
    }

    @Test
    void emptyListStillChecksMandatoryCurrentCustomerProvider() {
        var orders = mock(OrderReadStore.class);
        var customers = mock(OrderCustomerFacts.class);
        var service = new OrderQueryService(orders, customers);
        when(orders.findAll()).thenReturn(List.of());
        when(customers.current(Set.of())).thenReturn(Map.of());

        assertThat(service.list()).isEmpty();

        var sequence = inOrder(orders, customers);
        sequence.verify(orders).findAll();
        sequence.verify(customers).current(Set.of());
        verifyNoMoreInteractions(orders, customers);
    }

    @Test
    void missingOrderDoesNotReadCustomerFacts() {
        var orders = mock(OrderReadStore.class);
        var customers = mock(OrderCustomerFacts.class);
        var service = new OrderQueryService(orders, customers);
        UUID id = UUID.randomUUID();
        when(orders.findById(id)).thenReturn(Optional.empty());

        assertThatExceptionOfType(OrderFailure.class)
                .isThrownBy(() -> service.get(id))
                .satisfies(failure -> assertThat(failure.reason()).isEqualTo(OrderFailure.Reason.NOT_FOUND));

        verifyNoInteractions(customers);
    }

    @Test
    void missingReferencedCurrentCustomerRemainsAnIntegrityFailure() {
        var orders = mock(OrderReadStore.class);
        var customers = mock(OrderCustomerFacts.class);
        var service = new OrderQueryService(orders, customers);
        UUID id = UUID.randomUUID(), customer = UUID.randomUUID();
        var root = snapshot(customer, UUID.randomUUID(), "Historical", "1.00", "1.00");
        when(orders.findById(id)).thenReturn(Optional.of(root));
        when(customers.current(Set.of(customer))).thenReturn(Map.of());

        assertThatIllegalStateException()
                .isThrownBy(() -> service.get(id))
                .withMessage("Referenced customer is missing");
    }

    private static OrderReadSnapshot snapshot(UUID customer, UUID product, String name, String subtotal, String total) {
        return new OrderReadSnapshot(
                UUID.randomUUID(),
                customer,
                List.of(new OrderResult.Item(product, name, 1, new BigDecimal(subtotal), new BigDecimal(subtotal))),
                "EUR",
                new BigDecimal(subtotal),
                new BigDecimal(subtotal).subtract(new BigDecimal(total)),
                new BigDecimal(total));
    }
}
