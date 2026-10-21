package com.example.agenticorderservice.order.integration;

import com.example.agenticorderservice.customer.application.CustomerResult;
import com.example.agenticorderservice.customer.application.port.in.CustomerFacts;
import com.example.agenticorderservice.order.application.port.out.OrderCustomerFacts;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrderReadCustomerAdapter implements OrderCustomerFacts {
    private final CustomerFacts customers;

    public OrderReadCustomerAdapter(CustomerFacts customers) {
        this.customers = customers;
    }

    @Override
    public Map<UUID, CustomerResult> current(Collection<UUID> ids) {
        return customers.current(ids);
    }
}
