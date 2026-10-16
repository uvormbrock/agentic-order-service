package com.example.agenticorderservice.customer.application;

import com.example.agenticorderservice.customer.domain.Customer;
import com.example.agenticorderservice.customer.persistence.CustomerRepository;
import com.example.agenticorderservice.shared.application.InputValidationException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerApplicationService {
    private final CustomerRepository repository;
    private final com.example.agenticorderservice.order.application.OrderReferenceQuery orders;

    public CustomerApplicationService(
            CustomerRepository repository,
            com.example.agenticorderservice.order.application.OrderReferenceQuery orders) {
        this.repository = repository;
        this.orders = orders;
    }

    @Transactional
    public CustomerResult create(String name, String email) {
        String normalized = InputValidationException.name(name);
        var validated = validateEmail(email);
        return CustomerResult.from(repository.save(new Customer(normalized, validated)));
    }

    @Transactional(readOnly = true)
    public CustomerResult get(UUID id) {
        return CustomerResult.from(find(id));
    }

    @Transactional(readOnly = true)
    public List<CustomerResult> list() {
        return repository.findAll().stream().map(CustomerResult::from).toList();
    }

    @Transactional
    public CustomerResult update(UUID id, String name, String email) {
        String normalized = InputValidationException.name(name);
        var validated = validateEmail(email);
        var entity = findLocked(id);
        entity.update(normalized, validated);
        return CustomerResult.from(entity);
    }

    @Transactional
    public CustomerResult deactivate(UUID id) {
        var entity = findLocked(id);
        if (entity.isActive()) {
            entity.deactivate();
        }
        return CustomerResult.from(entity);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void delete(UUID id) {
        var entity = findLocked(id);
        // A distinct statement after the lock sees a placement that committed while we waited.
        if (orders.references(id)) {
            if (entity.isActive()) {
                entity.deactivate();
            }
        } else {
            repository.delete(entity);
        }
    }
    /** Shared by transport validation and direct application callers; no persistence access. */
    public static com.example.agenticorderservice.customer.domain.EmailAddress validateEmail(String value) {
        if (value == null) {
            throw new InputValidationException(InputValidationException.FieldFailure.REQUIRED);
        }
        try {
            return new com.example.agenticorderservice.customer.domain.EmailAddress(value);
        } catch (IllegalArgumentException failure) {
            throw new InputValidationException(InputValidationException.FieldFailure.EMAIL);
        }
    }

    private Customer findLocked(UUID id) {
        return repository.findLockedById(id).orElseThrow(CustomerNotFoundException::new);
    }

    private Customer find(UUID id) {
        return repository.findById(id).orElseThrow(CustomerNotFoundException::new);
    }
}
