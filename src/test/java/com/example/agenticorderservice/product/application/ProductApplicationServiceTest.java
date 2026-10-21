package com.example.agenticorderservice.product.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.product.application.port.out.ProductCatalogReads;
import com.example.agenticorderservice.product.domain.*;
import com.example.agenticorderservice.shared.domain.Money;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;

class ProductApplicationServiceTest {
    private final ProductCatalogReads repository = mock(ProductCatalogReads.class);
    private final com.example.agenticorderservice.product.application.port.in.GetProductQuery query =
            mock(com.example.agenticorderservice.product.application.port.in.GetProductQuery.class);
    private final com.example.agenticorderservice.product.application.port.in.ProductWriteOperations writes =
            mock(com.example.agenticorderservice.product.application.port.in.ProductWriteOperations.class);
    private final ProductApplicationService service = new ProductApplicationService(repository, query, writes);
    private final UUID id = UUID.randomUUID();

    @Test
    void priceValidatorCompatibilityMethodReturnsValidatedMoney() {
        assertThat(ProductApplicationService.validatePrice(new BigDecimal("12.3")))
                .isEqualTo(Money.of("12.30"));
    }

    @Test
    void writeCompatibilityMethodsDelegateWithoutRepositoryAccess() {
        var result = new ProductResult(id, "Name", new BigDecimal("12.34"), false);
        when(writes.create("Name", new BigDecimal("12.34"))).thenReturn(result);
        when(writes.update(id, "Name", new BigDecimal("12.34"))).thenReturn(result);
        when(writes.deactivate(id)).thenReturn(result);
        assertThat(service.create("Name", new BigDecimal("12.34"))).isSameAs(result);
        assertThat(service.update(id, "Name", new BigDecimal("12.34"))).isSameAs(result);
        assertThat(service.deactivate(id)).isSameAs(result);
        verify(writes).create("Name", new BigDecimal("12.34"));
        verify(writes).update(id, "Name", new BigDecimal("12.34"));
        verify(writes).deactivate(id);
        verifyNoMoreInteractions(writes);
        verifyNoInteractions(repository, query);
    }

    @Test
    void missingReadResourceRetainsFeatureFailureThroughCompatibilityDelegate() {
        when(query.get(id)).thenThrow(new ProductNotFoundException());
        assertThatThrownBy(() -> service.get(id)).isInstanceOf(ProductNotFoundException.class);
        verify(query).get(id);
        verifyNoInteractions(repository, writes);
    }

    @Test
    void listsActiveAndInactiveAndReturnsImmutableResults() {
        var active = new Product("First", Money.of("12.34"));
        var inactive = new Product("Second", Money.of("12.34"));
        inactive.deactivate();
        when(repository.findAll()).thenReturn(List.of(ProductResult.from(active), ProductResult.from(inactive)));
        var results = service.list();
        assertThat(results).hasSize(2);
        assertThat(results).extracting(ProductResult::active).containsExactly(true, false);
        assertThatThrownBy(results::clear).isInstanceOf(UnsupportedOperationException.class);
        when(repository.findAll()).thenReturn(List.of());
        assertThat(service.list()).isEmpty();
    }

    @Test
    void resultIsSnapshotIndependentOfLaterMutation() {
        var entity = new Product("Original", Money.of("12.34"));
        when(query.get(id)).thenReturn(ProductResult.from(entity));
        var result = service.get(id);
        entity.update("Changed", Money.of("12.34"));
        entity.deactivate();
        assertThat(result.name()).isEqualTo("Original");
        assertThat(result.active()).isTrue();
        verify(query).get(id);
        verifyNoInteractions(repository);
    }
}
