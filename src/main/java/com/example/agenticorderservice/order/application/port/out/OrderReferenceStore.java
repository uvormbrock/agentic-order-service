package com.example.agenticorderservice.order.application.port.out;

import java.util.UUID;

public interface OrderReferenceStore {
    boolean references(UUID customerId);
}
