package com.example.agenticorderservice.customer.application;
import com.example.agenticorderservice.customer.domain.*;
import com.example.agenticorderservice.customer.persistence.CustomerRepository;
import com.example.agenticorderservice.order.application.OrderReferenceQuery;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class CustomerDeletionTest {
    @ParameterizedTest @CsvSource({"false,true","true,true","true,false"})
    void locksBeforeReferenceQueryAndDeletesOrDeactivates(boolean referenced,boolean active) {
        var repository=mock(CustomerRepository.class);var orders=mock(OrderReferenceQuery.class);var service=new CustomerApplicationService(repository,orders);
        var customer=new Customer("C",new EmailAddress("a@example.com"));if(!active)customer.deactivate();UUID id=UUID.randomUUID();
        when(repository.findLockedById(id)).thenReturn(Optional.of(customer));when(orders.references(id)).thenReturn(referenced);
        service.delete(id);var ordered=inOrder(repository,orders);ordered.verify(repository).findLockedById(id);ordered.verify(orders).references(id);
        if(referenced) {assertThat(customer.isActive()).isFalse();verify(repository,never()).delete(any());}
        else {verify(repository).delete(customer);assertThat(customer.isActive()).isTrue();}
    }
    @Test void absentCustomerDoesNotQueryReferences() {
        var repository=mock(CustomerRepository.class);var orders=mock(OrderReferenceQuery.class);
        assertThatExceptionOfType(CustomerNotFoundException.class).isThrownBy(() -> new CustomerApplicationService(repository,orders).delete(UUID.randomUUID()));
        verifyNoInteractions(orders);
    }
}
