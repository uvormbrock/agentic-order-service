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
    public CustomerApplicationService(CustomerRepository repository) { this.repository = repository; }
    @Transactional
    public CustomerResult create(String name, String email) {
        String normalized = InputValidationException.name(name);
        var validated = validateEmail(email);
        return CustomerResult.from(repository.save(new Customer(normalized, validated)));
    }
    @Transactional(readOnly = true)
    public CustomerResult get(UUID id) { return CustomerResult.from(find(id)); }
    @Transactional(readOnly = true)
    public List<CustomerResult> list() { return repository.findAll().stream().map(CustomerResult::from).toList(); }
    @Transactional
    public CustomerResult update(UUID id, String name, String email) {
        String normalized = InputValidationException.name(name);
        var validated = validateEmail(email);
        var entity = find(id);
        entity.update(normalized, validated);
        return CustomerResult.from(entity);
    }
    @Transactional
    public CustomerResult deactivate(UUID id) {
        var entity = find(id);
        if (entity.isActive()) { entity.deactivate(); }
        return CustomerResult.from(entity);
    }
    /** Shared by transport validation and direct application callers; no persistence access. */
    public static com.example.agenticorderservice.customer.domain.EmailAddress validateEmail(String value) {
        if (value == null) { throw new InputValidationException(InputValidationException.FieldFailure.REQUIRED); }
        try { return new com.example.agenticorderservice.customer.domain.EmailAddress(value); }
        catch (IllegalArgumentException failure) { throw new InputValidationException(InputValidationException.FieldFailure.EMAIL); }
    }
    private Customer find(UUID id) { return repository.findById(id).orElseThrow(CustomerNotFoundException::new); }
}
