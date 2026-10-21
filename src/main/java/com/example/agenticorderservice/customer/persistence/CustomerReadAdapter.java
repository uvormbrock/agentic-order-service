package com.example.agenticorderservice.customer.persistence;

import com.example.agenticorderservice.customer.application.CustomerNotFoundException;
import com.example.agenticorderservice.customer.application.CustomerResult;
import com.example.agenticorderservice.customer.application.port.out.CustomerReadStore;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CustomerReadAdapter implements CustomerReadStore {
    private final CustomerRepository repository;

    public CustomerReadAdapter(CustomerRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CustomerResult> findById(UUID id) {
        return repository.findById(id).map(CustomerResult::from);
    }

    @Override
    public List<CustomerResult> findAll() {
        return repository.findAll().stream().map(CustomerResult::from).toList();
    }

    @Override
    public Map<UUID, CustomerResult> findCurrentByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return repository.findByIdIn(ids).stream()
                .map(CustomerResult::from)
                .collect(Collectors.toUnmodifiableMap(CustomerResult::id, value -> value));
    }

    @Override
    public CustomerResult lock(UUID id) {
        return CustomerResult.from(repository.findLockedById(id).orElseThrow(CustomerNotFoundException::new));
    }
}
