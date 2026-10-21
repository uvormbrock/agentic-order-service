package com.example.agenticorderservice.customer.application.port.out;

import com.example.agenticorderservice.customer.domain.Customer;
import java.util.Optional;
import java.util.UUID;

/** Customer-only managed aggregate access within the owning application transaction. */
public interface CustomerWriteStore {
    Customer save(Customer customer);

    Optional<Customer> findLockedById(UUID id);

    void delete(Customer customer);
}
