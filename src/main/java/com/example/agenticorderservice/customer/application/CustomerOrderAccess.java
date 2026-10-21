package com.example.agenticorderservice.customer.application;

import com.example.agenticorderservice.customer.application.port.in.CustomerFacts;
import com.example.agenticorderservice.customer.application.port.out.CustomerReadStore;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Customer-owned immutable facts; the caller owns the transaction. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class CustomerOrderAccess implements CustomerFacts {
    private final CustomerReadStore reads;

    public CustomerOrderAccess(CustomerReadStore reads) {
        this.reads = reads;
    }

    @Override
    public CustomerResult lock(UUID id) {
        return reads.lock(id);
    }

    @Override
    public Map<UUID, CustomerResult> current(Collection<UUID> ids) {
        return reads.findCurrentByIds(ids);
    }
}
