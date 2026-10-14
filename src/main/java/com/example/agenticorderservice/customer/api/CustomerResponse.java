package com.example.agenticorderservice.customer.api;
import java.util.UUID;
import com.example.agenticorderservice.customer.application.CustomerResult;
public record CustomerResponse(UUID id, String name, String email, boolean active) {
    static CustomerResponse from(CustomerResult result) {
        return new CustomerResponse(result.id(), result.name(), result.email(), result.active());
    }
}
