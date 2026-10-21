package com.example.agenticorderservice.customer.persistence;

import com.example.agenticorderservice.customer.application.port.out.CustomerWriteStore;
import com.example.agenticorderservice.customer.domain.Customer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CustomerWriteAdapter implements CustomerWriteStore {
    private final CustomerRepository repository;

    public CustomerWriteAdapter(CustomerRepository repository) {
        this.repository = repository;
    }

    @Override
    public Customer save(Customer customer) {
        return repository.save(customer);
    }

    @Override
    public Optional<Customer> findLockedById(UUID id) {
        return repository.findLockedById(id);
    }

    @Override
    public void delete(Customer customer) {
        repository.delete(customer);
    }
}
