package com.example.agenticorderservice.order.integration;

import com.example.agenticorderservice.customer.application.CustomerResult;
import com.example.agenticorderservice.customer.application.port.in.CustomerFacts;
import com.example.agenticorderservice.order.application.port.out.OrderPlacementCustomerFacts;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrderPlacementCustomerAdapter implements OrderPlacementCustomerFacts {
    private final CustomerFacts customers;

    public OrderPlacementCustomerAdapter(CustomerFacts customers) {
        this.customers = customers;
    }

    @Override
    public CustomerResult lock(UUID id) {
        return customers.lock(id);
    }
}
