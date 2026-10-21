package com.example.agenticorderservice.product.persistence;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.product.domain.Product;
import com.example.agenticorderservice.shared.domain.Money;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductWriteAdapterTest {
    private final ProductRepository repository = mock(ProductRepository.class);
    private final ProductWriteAdapter adapter = new ProductWriteAdapter(repository);

    @Test
    void saveReturnsRepositoryAssignedAggregateWithoutReplacingItsIdentity() {
        var supplied = new Product("Name", Money.of("12.34"));
        var assigned = mock(Product.class);
        when(repository.save(supplied)).thenReturn(assigned);
        assertThat(adapter.save(supplied)).isSameAs(assigned);
        verify(repository).save(supplied);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void lockedLookupPreservesManagedAggregateAndMissingResultWithoutFallbackReads() {
        UUID id = UUID.randomUUID();
        var managed = new Product("Name", Money.of("12.34"));
        when(repository.findLockedById(id)).thenReturn(Optional.of(managed), Optional.empty());
        assertThat(adapter.findLockedById(id)).containsSame(managed);
        assertThat(adapter.findLockedById(id)).isEmpty();
        verify(repository, times(2)).findLockedById(id);
        verifyNoMoreInteractions(repository);
    }
}
