package com.example.agenticorderservice.customer.persistence;

import com.example.agenticorderservice.customer.domain.Customer;
import org.springframework.data.repository.Repository;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends Repository<Customer, UUID> {
    java.util.List<Customer> findAll();
    Customer save(Customer entity);
    Optional<Customer> findById(UUID id);
}
