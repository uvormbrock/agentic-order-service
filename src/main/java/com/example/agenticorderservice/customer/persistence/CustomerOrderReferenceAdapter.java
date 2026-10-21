package com.example.agenticorderservice.customer.persistence;

import com.example.agenticorderservice.customer.application.port.out.CustomerOrderReferences;
import com.example.agenticorderservice.order.application.port.in.OrderReferences;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Bridges the Order-owned reference capability within the caller's transaction. */
@Component
public class CustomerOrderReferenceAdapter implements CustomerOrderReferences {
    private final OrderReferences orders;

    public CustomerOrderReferenceAdapter(OrderReferences orders) {
        this.orders = orders;
    }

    @Override
    public boolean references(UUID customerId) {
        return orders.references(customerId);
    }
}
