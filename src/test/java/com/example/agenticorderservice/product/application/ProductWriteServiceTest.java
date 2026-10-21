package com.example.agenticorderservice.product.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.product.domain.*;
import com.example.agenticorderservice.shared.application.InputValidationException;
import com.example.agenticorderservice.shared.domain.Money;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;

class ProductWriteServiceTest {
    private final com.example.agenticorderservice.product.application.port.out.ProductWriteStore repository =
            mock(com.example.agenticorderservice.product.application.port.out.ProductWriteStore.class);
    private final ProductWriteService service = new ProductWriteService(repository);
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
    void invalidNamePrecedesInvalidPriceAndMissingResourceLookup() {
        assertThatExceptionOfType(InputValidationException.class)
                .isThrownBy(() -> service.update(id, " ", new BigDecimal("-1")))
                .satisfies(exception ->
                        assertThat(exception.failure()).isEqualTo(InputValidationException.FieldFailure.NAME));
        verifyNoInteractions(repository);
    }

    @Test
    void createdResultPreservesPriceAndIsIndependentOfLaterAggregateMutation() {
        var saved = new Product[1];
        when(repository.save(any())).thenAnswer(call -> {
            saved[0] = call.getArgument(0);
            return saved[0];
        });
        var result = service.create(" Original ", new BigDecimal("45.67"));
        saved[0].update("Changed", Money.of("1.00"));
        saved[0].deactivate();
        assertThat(result.name()).isEqualTo("Original");
        assertThat(result.price()).isEqualByComparingTo("45.67");
        assertThat(result.active()).isTrue();
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
}
