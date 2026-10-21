package com.example.agenticorderservice.customer.application.port.in;

import com.example.agenticorderservice.customer.application.CustomerResult;
import java.util.List;
import java.util.UUID;

public interface CustomerOperations {
    CustomerResult create(String name, String email);

    CustomerResult get(UUID id);

    List<CustomerResult> list();

    CustomerResult update(UUID id, String name, String email);

    CustomerResult deactivate(UUID id);

    void delete(UUID id);
}
