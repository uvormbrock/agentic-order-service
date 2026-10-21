package com.example.agenticorderservice.customer.application.port.in;

import com.example.agenticorderservice.customer.application.CustomerResult;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Immutable Customer facts participating in the caller's transaction. */
public interface CustomerFacts {
    CustomerResult lock(UUID id);

    Map<UUID, CustomerResult> current(Collection<UUID> ids);
}
