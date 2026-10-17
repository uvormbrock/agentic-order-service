package com.example.agenticorderservice.customer.api;

import com.example.agenticorderservice.customer.application.CustomerResult;
import java.util.UUID;

public record CustomerResponse(UUID id, String name, String email, boolean active) {
    static CustomerResponse from(CustomerResult result) {
        return new CustomerResponse(result.id(), result.name(), result.email(), result.active());
    }
}
