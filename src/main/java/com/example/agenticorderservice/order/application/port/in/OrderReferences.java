package com.example.agenticorderservice.order.application.port.in;

import java.util.UUID;

/** Order-owned reference facts participating in the caller's transaction. */
public interface OrderReferences {
    boolean references(UUID customerId);
}
