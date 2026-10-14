package com.example.agenticorderservice.customer.application;
import java.util.UUID;
import com.example.agenticorderservice.customer.domain.Customer;
public record CustomerResult(UUID id, String name, String email, boolean active) {
    static CustomerResult from(Customer entity) {
        return new CustomerResult(entity.getId(), entity.getName(), entity.getEmail().value(), entity.isActive());
    }
}
