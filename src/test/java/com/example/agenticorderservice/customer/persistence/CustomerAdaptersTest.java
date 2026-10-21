package com.example.agenticorderservice.customer.persistence;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.customer.application.*;
import com.example.agenticorderservice.customer.domain.*;
import com.example.agenticorderservice.order.application.port.in.OrderReferences;
import java.util.*;
import org.junit.jupiter.api.Test;

class CustomerAdaptersTest {
    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final CustomerReadAdapter reads = new CustomerReadAdapter(repository);
    private final CustomerWriteAdapter writes = new CustomerWriteAdapter(repository);

    @Test
    void readsProduceImmutableSnapshotsAndPreserveMissingAndLockSemantics() {
        UUID id = UUID.randomUUID();
        var customer = new Customer("Original", new EmailAddress("a@example.com"));
        when(repository.findById(id)).thenReturn(Optional.of(customer));
        when(repository.findAll()).thenReturn(List.of(customer));
        when(repository.findLockedById(id)).thenReturn(Optional.of(customer));
        var found = reads.findById(id).orElseThrow();
        var all = reads.findAll();
        assertThat(reads.lock(id)).isEqualTo(found);
        customer.update("Changed", new EmailAddress("b@example.com"));
        assertThat(found.name()).isEqualTo("Original");
        assertThat(found.email()).isEqualTo("a@example.com");
        assertThat(all).containsExactly(found);
        assertThatThrownBy(all::clear).isInstanceOf(UnsupportedOperationException.class);
        UUID absent = UUID.randomUUID();
        assertThat(reads.findById(absent)).isEmpty();
        assertThatExceptionOfType(CustomerNotFoundException.class).isThrownBy(() -> reads.lock(absent));
        verify(repository).findLockedById(id);
        verify(repository).findLockedById(absent);
    }

    @Test
    void currentFactsSkipEmptyQueryAndRemainImmutableSnapshots() {
        assertThat(reads.findCurrentByIds(List.of())).isEmpty();
        verifyNoInteractions(repository);
        var customer = new Customer("Original", new EmailAddress("a@example.com"));
        org.springframework.test.util.ReflectionTestUtils.setField(customer, "id", UUID.randomUUID());
        var ids = List.of(customer.getId());
        when(repository.findByIdIn(ids)).thenReturn(List.of(customer));
        var facts = reads.findCurrentByIds(ids);
        assertThat(facts).containsEntry(customer.getId(), CustomerResult.from(customer));
        assertThatThrownBy(facts::clear).isInstanceOf(UnsupportedOperationException.class);
        customer.deactivate();
        assertThat(facts.get(customer.getId()).active()).isTrue();
    }

    @Test
    void writesRetainManagedIdentityAndDelegateOnlyRequiredPersistenceCapabilities() {
        var customer = new Customer("Original", new EmailAddress("a@example.com"));
        when(repository.save(customer)).thenReturn(customer);
        when(repository.findLockedById(customer.getId())).thenReturn(Optional.of(customer));
        assertThat(writes.save(customer)).isSameAs(customer);
        assertThat(writes.findLockedById(customer.getId()).orElseThrow()).isSameAs(customer);
        writes.delete(customer);
        verify(repository).save(customer);
        verify(repository).findLockedById(customer.getId());
        verify(repository).delete(customer);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void referenceBridgePreservesTrueFalseAndProviderFailure() {
        var orders = mock(OrderReferences.class);
        var bridge = new CustomerOrderReferenceAdapter(orders);
        UUID id = UUID.randomUUID();
        var failure = new IllegalStateException("provider unavailable");
        when(orders.references(id)).thenReturn(true, false).thenThrow(failure);
        assertThat(bridge.references(id)).isTrue();
        assertThat(bridge.references(id)).isFalse();
        assertThatThrownBy(() -> bridge.references(id)).isSameAs(failure);
        verify(orders, times(3)).references(id);
        verifyNoMoreInteractions(orders);
    }
}
