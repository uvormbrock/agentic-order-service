package com.example.agenticorderservice.product.persistence;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.product.application.ProductResult;
import com.example.agenticorderservice.product.domain.Product;
import com.example.agenticorderservice.shared.domain.Money;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProductReadAdapterTest {
    private final ProductRepository repository = mock(ProductRepository.class);
    private final ProductReadAdapter adapter = new ProductReadAdapter(repository);

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void materializesEveryFactWithoutLeakingManagedState(boolean active) {
        UUID id = UUID.randomUUID();
        Product entity = mock(Product.class);
        when(entity.getId()).thenReturn(id);
        when(entity.getName()).thenReturn("Äpfel😀");
        when(entity.getPrice()).thenReturn(Money.of("12.34"));
        when(entity.isActive()).thenReturn(active);
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        var result = adapter.findById(id).orElseThrow();
        assertThat(result).isEqualTo(new ProductResult(id, "Äpfel😀", new BigDecimal("12.34"), active));
        when(entity.getName()).thenReturn("Changed");
        when(entity.getPrice()).thenReturn(Money.of("99.99"));
        when(entity.isActive()).thenReturn(!active);
        assertThat(result.name()).isEqualTo("Äpfel😀");
        assertThat(result.price()).isEqualByComparingTo("12.34");
        assertThat(result.active()).isEqualTo(active);
        verify(repository).findById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void missingProductReturnsEmptyWithoutWrites() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThat(adapter.findById(id)).isEmpty();
        verify(repository).findById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void listsActiveAndInactiveFactsAsImmutableSnapshotsAndPreservesEmptyResults() {
        var active = new Product("Original", Money.of("12.34"));
        var inactive = new Product("Inactive", Money.of("4.56"));
        inactive.deactivate();
        when(repository.findAll()).thenReturn(java.util.List.of(active, inactive));
        var results = adapter.findAll();
        assertThat(results)
                .containsExactly(
                        new ProductResult(null, "Original", new BigDecimal("12.34"), true),
                        new ProductResult(null, "Inactive", new BigDecimal("4.56"), false));
        assertThatThrownBy(results::clear).isInstanceOf(UnsupportedOperationException.class);
        active.update("Changed", Money.of("99.99"));
        active.deactivate();
        assertThat(results.getFirst()).isEqualTo(new ProductResult(null, "Original", new BigDecimal("12.34"), true));
        when(repository.findAll()).thenReturn(java.util.List.of());
        assertThat(adapter.findAll()).isEmpty();
        verify(repository, times(2)).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void lockedFactsPreserveInactiveStateMissingFailureAndOriginalProviderFailure() {
        UUID id = UUID.randomUUID();
        var product = new Product("Inactive", Money.of("12.34"));
        product.deactivate();
        var failure = new IllegalStateException("provider failure");
        when(repository.findLockedById(id))
                .thenReturn(Optional.of(product), Optional.empty())
                .thenThrow(failure);
        assertThat(adapter.lock(id)).isEqualTo(new ProductResult(null, "Inactive", new BigDecimal("12.34"), false));
        assertThatThrownBy(() -> adapter.lock(id))
                .isInstanceOf(com.example.agenticorderservice.product.application.ProductNotFoundException.class);
        assertThatThrownBy(() -> adapter.lock(id)).isSameAs(failure);
        verify(repository, times(3)).findLockedById(id);
        verifyNoMoreInteractions(repository);
    }
}
