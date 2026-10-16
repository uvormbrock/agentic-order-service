package com.example.agenticorderservice.product.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.product.domain.*;
import com.example.agenticorderservice.product.persistence.ProductRepository;
import com.example.agenticorderservice.shared.application.InputValidationException;
import com.example.agenticorderservice.shared.domain.Money;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;

class ProductApplicationServiceTest {
    private final ProductRepository repository = mock(ProductRepository.class);
    private final ProductApplicationService service = new ProductApplicationService(repository);
    private final UUID id = UUID.randomUUID();

    @Test
    void createsNormalizedActiveResource() {
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        var result = service.create("  Älice😀  ", new BigDecimal("12.34"));
        assertThat(result.name()).isEqualTo("Älice😀");
        assertThat(result.active()).isTrue();
        verify(repository).save(any());
    }

    @Test
    void invalidInputNeverReadsOrWritesRepository() {
        var validPrice = new BigDecimal("12.34");
        var invalidPrice = new BigDecimal("12.345");
        assertThatThrownBy(() -> service.create("", validPrice)).isInstanceOf(InputValidationException.class);
        assertThatThrownBy(() -> service.update(id, "Valid", invalidPrice))
                .isInstanceOf(InputValidationException.class);
        assertThatExceptionOfType(InputValidationException.class)
                .isThrownBy(() -> service.create("Valid", null))
                .satisfies(exception ->
                        assertThat(exception.failure()).isEqualTo(InputValidationException.FieldFailure.REQUIRED));
        assertThatExceptionOfType(InputValidationException.class)
                .isThrownBy(() -> service.update(id, "Valid", null))
                .satisfies(exception ->
                        assertThat(exception.failure()).isEqualTo(InputValidationException.FieldFailure.REQUIRED));
        verifyNoInteractions(repository);
    }

    @Test
    void missingResourceRaisesFeatureFailure() {
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(id)).isInstanceOf(ProductNotFoundException.class);
        var validPrice = new BigDecimal("12.34");
        assertThatThrownBy(() -> service.update(id, "Valid", validPrice)).isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> service.deactivate(id)).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void updatesInactiveResourceAndPreservesActivity() {
        var entity = new Product("Original", Money.of("12.34"));
        entity.deactivate();
        when(repository.findLockedById(id)).thenReturn(Optional.of(entity));
        var originalId = entity.getId();
        var result = service.update(id, "Changed", new BigDecimal("45.67"));
        assertThat(result.price()).isEqualByComparingTo("45.67");
        assertThat(entity.getPrice()).isEqualTo(Money.of("45.67"));
        assertThat(result.id()).isEqualTo(originalId);
        assertThat(entity.getId()).isEqualTo(originalId);
        assertThat(entity.getName()).isEqualTo("Changed");
        assertThat(entity.isActive()).isFalse();
        assertThat(result.name()).isEqualTo("Changed");
        assertThat(result.active()).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void repeatedDeactivationIsUnchangedAndDoesNotSave() {
        var entity = new Product("Original", Money.of("12.34"));
        when(repository.findLockedById(id)).thenReturn(Optional.of(entity));
        var first = service.deactivate(id);
        var second = service.deactivate(id);
        assertThat(second).isEqualTo(first);
        assertThat(second.active()).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void listsActiveAndInactiveAndReturnsImmutableResults() {
        var active = new Product("First", Money.of("12.34"));
        var inactive = new Product("Second", Money.of("12.34"));
        inactive.deactivate();
        when(repository.findAll()).thenReturn(List.of(active, inactive));
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
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        var result = service.get(id);
        entity.update("Changed", Money.of("12.34"));
        entity.deactivate();
        assertThat(result.name()).isEqualTo("Original");
        assertThat(result.active()).isTrue();
    }
}
