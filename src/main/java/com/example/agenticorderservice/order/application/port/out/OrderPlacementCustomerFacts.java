package com.example.agenticorderservice.order.application.port.out;

import com.example.agenticorderservice.customer.application.CustomerResult;
import java.util.UUID;

public interface OrderPlacementCustomerFacts {
    CustomerResult lock(UUID id);
}
