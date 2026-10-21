package com.example.agenticorderservice.customer.application;

import com.example.agenticorderservice.customer.application.port.in.CustomerOperations;
import com.example.agenticorderservice.customer.application.port.out.CustomerOrderReferences;
import com.example.agenticorderservice.customer.application.port.out.CustomerReadStore;
import com.example.agenticorderservice.customer.application.port.out.CustomerWriteStore;
import com.example.agenticorderservice.customer.domain.Customer;
import com.example.agenticorderservice.shared.application.InputValidationException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerApplicationService implements CustomerOperations {
    private final CustomerReadStore reads;
    private final CustomerWriteStore writes;
    private final CustomerOrderReferences orders;

    public CustomerApplicationService(
            CustomerReadStore reads, CustomerWriteStore writes, CustomerOrderReferences orders) {
        this.reads = reads;
        this.writes = writes;
        this.orders = orders;
    }

    @Override
    @Transactional
    public CustomerResult create(String name, String email) {
        String normalized = InputValidationException.name(name);
        var validated = validateEmail(email);
        return CustomerResult.from(writes.save(new Customer(normalized, validated)));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResult get(UUID id) {
        return reads.findById(id).orElseThrow(CustomerNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResult> list() {
        return reads.findAll();
    }

    @Override
    @Transactional
    public CustomerResult update(UUID id, String name, String email) {
        String normalized = InputValidationException.name(name);
        var validated = validateEmail(email);
        var entity = findLocked(id);
        entity.update(normalized, validated);
        return CustomerResult.from(entity);
    }

    @Override
    @Transactional
    public CustomerResult deactivate(UUID id) {
        var entity = findLocked(id);
        if (entity.isActive()) {
            entity.deactivate();
        }
        return CustomerResult.from(entity);
    }

    @Override
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void delete(UUID id) {
        var entity = findLocked(id);
        // A distinct statement after the lock sees a placement that committed while we waited.
        if (orders.references(id)) {
            throw new CustomerHasOrdersException();
        }
        writes.delete(entity);
        observeDeleteCommit("CUSTOMER_DELETED", org.slf4j.event.Level.INFO);
    }

    private static void observeDeleteCommit(String event, org.slf4j.event.Level level) {
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive())
            return;
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        com.example.agenticorderservice.shared.logging.SafeEvent.emit(
                                org.slf4j.LoggerFactory.getLogger(CustomerApplicationService.class),
                                level,
                                "event={}",
                                event);
                    }
                });
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

    public static String normalizeEmail(String value) {
        return validateEmail(value).value();
    }

    private Customer findLocked(UUID id) {
        return writes.findLockedById(id).orElseThrow(CustomerNotFoundException::new);
    }
}
