package com.example.agenticorderservice.customer.application.port.out;

import java.util.UUID;

public interface CustomerOrderReferences {
    boolean references(UUID customerId);
}
