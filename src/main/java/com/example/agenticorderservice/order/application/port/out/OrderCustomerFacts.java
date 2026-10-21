package com.example.agenticorderservice.order.application.port.out;

import com.example.agenticorderservice.customer.application.CustomerResult;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Current immutable customer facts participating in the owning Order read transaction. */
public interface OrderCustomerFacts {
    Map<UUID, CustomerResult> current(Collection<UUID> ids);
}
