package com.example.agenticorderservice.order.application;

import com.example.agenticorderservice.order.application.port.in.OrderReferences;
import com.example.agenticorderservice.order.application.port.out.OrderReferenceStore;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Independent Order-owned query, without a dependency on placement orchestration. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class OrderReferenceQuery implements OrderReferences {
    private final OrderReferenceStore references;

    public OrderReferenceQuery(OrderReferenceStore references) {
        this.references = references;
    }

    @Override
    public boolean references(UUID customerId) {
        return references.references(customerId);
    }
}
