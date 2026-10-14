package com.example.agenticorderservice.customer.application;
import com.example.agenticorderservice.customer.domain.*;
import com.example.agenticorderservice.customer.persistence.CustomerRepository;
import com.example.agenticorderservice.shared.domain.Money;
import com.example.agenticorderservice.shared.application.InputValidationException;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class CustomerApplicationServiceTest {
    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final CustomerApplicationService service = new CustomerApplicationService(repository);
    private final UUID id = UUID.randomUUID();
    @Test void createsNormalizedActiveResource() {
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        var result = service.create("  Älice😀  ", "user@example.com");
        assertThat(result.name()).isEqualTo("Älice😀"); assertThat(result.active()).isTrue();
        verify(repository).save(any());
    }
    @Test void invalidInputNeverReadsOrWritesRepository() {
        assertThatThrownBy(() -> service.create("", "user@example.com")).isInstanceOf(InputValidationException.class);
        assertThatThrownBy(() -> service.update(id, "Valid", "invalid")).isInstanceOf(InputValidationException.class);
        verifyNoInteractions(repository);
    }
    @Test void missingResourceRaisesFeatureFailure() {
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(id)).isInstanceOf(CustomerNotFoundException.class);
        assertThatThrownBy(() -> service.update(id, "Valid", "user@example.com")).isInstanceOf(CustomerNotFoundException.class);
        assertThatThrownBy(() -> service.deactivate(id)).isInstanceOf(CustomerNotFoundException.class);
    }
    @Test void updatesInactiveResourceAndPreservesActivity() {
        var entity = new Customer("Original", new EmailAddress("user@example.com")); entity.deactivate();
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        var result = service.update(id, "Changed", "user@example.com");
        assertThat(result.name()).isEqualTo("Changed"); assertThat(result.active()).isFalse();
        verify(repository, never()).save(any());
    }
    @Test void repeatedDeactivationIsUnchangedAndDoesNotSave() {
        var entity = new Customer("Original", new EmailAddress("user@example.com"));
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        var first = service.deactivate(id); var second = service.deactivate(id);
        assertThat(second).isEqualTo(first); assertThat(second.active()).isFalse();
        verify(repository, never()).save(any());
    }
    @Test void listsActiveAndInactiveAndReturnsImmutableResults() {
        var active = new Customer("First", new EmailAddress("user@example.com")); var inactive = new Customer("Second", new EmailAddress("user@example.com")); inactive.deactivate();
        when(repository.findAll()).thenReturn(List.of(active, inactive));
        var results = service.list(); assertThat(results).hasSize(2);
        assertThat(results).extracting(CustomerResult::active).containsExactly(true, false);
        assertThatThrownBy(() -> results.clear()).isInstanceOf(UnsupportedOperationException.class);
        when(repository.findAll()).thenReturn(List.of()); assertThat(service.list()).isEmpty();
    }
    @Test void resultIsSnapshotIndependentOfLaterMutation() {
        var entity = new Customer("Original", new EmailAddress("user@example.com")); when(repository.findById(id)).thenReturn(Optional.of(entity));
        var result = service.get(id); entity.update("Changed", new EmailAddress("user@example.com")); entity.deactivate();
        assertThat(result.name()).isEqualTo("Original"); assertThat(result.active()).isTrue();
    }
}
