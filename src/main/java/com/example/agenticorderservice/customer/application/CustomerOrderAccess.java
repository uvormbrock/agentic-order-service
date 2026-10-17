package com.example.agenticorderservice.customer.application;

import com.example.agenticorderservice.customer.persistence.CustomerRepository;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Customer-owned immutable facts; the caller owns the transaction. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class CustomerOrderAccess {
    private final CustomerRepository repository;

    public CustomerOrderAccess(CustomerRepository repository) {
        this.repository = repository;
    }

    public CustomerResult lock(UUID id) {
        return CustomerResult.from(repository.findLockedById(id).orElseThrow(CustomerNotFoundException::new));
    }

    public Map<UUID, CustomerResult> current(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return repository.findByIdIn(ids).stream()
                .map(CustomerResult::from)
                .collect(Collectors.toUnmodifiableMap(CustomerResult::id, value -> value));
    }
}
