package com.example.agenticorderservice.customer.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.customer.application.port.out.*;
import com.example.agenticorderservice.customer.domain.*;
import com.example.agenticorderservice.shared.application.InputValidationException;
import java.util.*;
import org.junit.jupiter.api.*;

class CustomerApplicationServiceTest {
    private final CustomerWriteStore repository = mock(CustomerWriteStore.class);
    private final CustomerReadStore reads = mock(CustomerReadStore.class);
    private final CustomerApplicationService service =
            new CustomerApplicationService(reads, repository, mock(CustomerOrderReferences.class));
    private final UUID id = UUID.randomUUID();

    @Test
    void normalizesEmailWithoutChangingValidationFailuresOrBoundaries() {
        assertThat(CustomerApplicationService.normalizeEmail("\u2003 Alice.Tag+Orders@Example.COM \t"))
                .isEqualTo("alice.tag+orders@example.com");
        String boundary = "A".repeat(242) + "@EXAMPLE.COM";
        assertThat(CustomerApplicationService.normalizeEmail(" " + boundary + " "))
                .isEqualTo("a".repeat(242) + "@example.com");
        for (String invalid :
                new String[] {"invalid", "a..b@example.com", "á@example.com", "a".repeat(243) + "@example.com"}) {
            assertThatExceptionOfType(InputValidationException.class)
                    .isThrownBy(() -> CustomerApplicationService.normalizeEmail(invalid))
                    .satisfies(exception ->
                            assertThat(exception.failure()).isEqualTo(InputValidationException.FieldFailure.EMAIL));
        }
        assertThatExceptionOfType(InputValidationException.class)
                .isThrownBy(() -> CustomerApplicationService.normalizeEmail(null))
                .satisfies(exception ->
                        assertThat(exception.failure()).isEqualTo(InputValidationException.FieldFailure.REQUIRED));
        verifyNoInteractions(repository, reads);
    }

    @Test
    void createsNormalizedActiveResource() {
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        var result = service.create("  Älice😀  ", "user@example.com");
        assertThat(result.name()).isEqualTo("Älice😀");
        assertThat(result.active()).isTrue();
        verify(repository).save(any());
    }

    @Test
    void invalidInputNeverReadsOrWritesRepository() {
        assertThatThrownBy(() -> service.create("", "user@example.com")).isInstanceOf(InputValidationException.class);
        assertThatThrownBy(() -> service.update(id, "Valid", "invalid")).isInstanceOf(InputValidationException.class);
        assertThatExceptionOfType(InputValidationException.class)
                .isThrownBy(() -> service.create(null, "user@example.com"))
                .satisfies(exception ->
                        assertThat(exception.failure()).isEqualTo(InputValidationException.FieldFailure.REQUIRED));
        assertThatExceptionOfType(InputValidationException.class)
                .isThrownBy(() -> service.update(id, null, "user@example.com"))
                .satisfies(exception ->
                        assertThat(exception.failure()).isEqualTo(InputValidationException.FieldFailure.REQUIRED));
        assertThatExceptionOfType(InputValidationException.class)
                .isThrownBy(() -> service.create("Valid", null))
                .satisfies(exception ->
                        assertThat(exception.failure()).isEqualTo(InputValidationException.FieldFailure.REQUIRED));
        assertThatExceptionOfType(InputValidationException.class)
                .isThrownBy(() -> service.update(id, "Valid", null))
                .satisfies(exception ->
                        assertThat(exception.failure()).isEqualTo(InputValidationException.FieldFailure.REQUIRED));
        verifyNoInteractions(repository, reads);
    }

    @Test
    void missingResourceRaisesFeatureFailure() {
        when(reads.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(id)).isInstanceOf(CustomerNotFoundException.class);
        assertThatThrownBy(() -> service.update(id, "Valid", "user@example.com"))
                .isInstanceOf(CustomerNotFoundException.class);
        assertThatThrownBy(() -> service.deactivate(id)).isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void updatesInactiveResourceAndPreservesActivity() {
        var entity = new Customer("Original", new EmailAddress("user@example.com"));
        entity.deactivate();
        when(repository.findLockedById(id)).thenReturn(Optional.of(entity));
        var originalId = entity.getId();
        var result = service.update(id, "Changed", " CHANGED+Tag@Example.COM ");
        assertThat(result.email()).isEqualTo("changed+tag@example.com");
        assertThat(entity.getEmail().value()).isEqualTo("changed+tag@example.com");
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
        var entity = new Customer("Original", new EmailAddress("user@example.com"));
        when(repository.findLockedById(id)).thenReturn(Optional.of(entity));
        var first = service.deactivate(id);
        var second = service.deactivate(id);
        assertThat(second).isEqualTo(first);
        assertThat(second.active()).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void listsActiveAndInactiveAndReturnsImmutableResults() {
        var active = new Customer("First", new EmailAddress("user@example.com"));
        var inactive = new Customer("Second", new EmailAddress("user@example.com"));
        inactive.deactivate();
        when(reads.findAll()).thenReturn(List.of(CustomerResult.from(active), CustomerResult.from(inactive)));
        var results = service.list();
        assertThat(results).hasSize(2);
        assertThat(results).extracting(CustomerResult::active).containsExactly(true, false);
        assertThatThrownBy(results::clear).isInstanceOf(UnsupportedOperationException.class);
        when(reads.findAll()).thenReturn(List.of());
        assertThat(service.list()).isEmpty();
    }

    @Test
    void resultIsSnapshotIndependentOfLaterMutation() {
        var entity = new Customer("Original", new EmailAddress("user@example.com"));
        when(reads.findById(id)).thenReturn(Optional.of(CustomerResult.from(entity)));
        var result = service.get(id);
        entity.update("Changed", new EmailAddress("user@example.com"));
        entity.deactivate();
        assertThat(result.name()).isEqualTo("Original");
        assertThat(result.active()).isTrue();
    }
}
