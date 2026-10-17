package com.example.agenticorderservice.customer.persistence;

import com.example.agenticorderservice.customer.domain.Customer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface CustomerRepository extends Repository<Customer, UUID> {
    java.util.List<Customer> findAll();

    Customer save(Customer entity);

    Optional<Customer> findById(UUID id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Customer e where e.id = :id")
    Optional<Customer> findLockedById(@org.springframework.data.repository.query.Param("id") UUID id);

    java.util.List<Customer> findByIdIn(java.util.Collection<UUID> ids);

    void delete(Customer entity);
}
