package com.example.agenticorderservice.customer.application.port.out;

import com.example.agenticorderservice.customer.application.CustomerResult;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CustomerReadStore {
    Optional<CustomerResult> findById(UUID id);

    List<CustomerResult> findAll();

    Map<UUID, CustomerResult> findCurrentByIds(Collection<UUID> ids);

    CustomerResult lock(UUID id);
}
