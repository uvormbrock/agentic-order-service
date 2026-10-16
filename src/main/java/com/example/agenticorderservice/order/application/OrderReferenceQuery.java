package com.example.agenticorderservice.order.application;

import com.example.agenticorderservice.order.persistence.OrderRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Independent Order-owned query, without a dependency on placement orchestration. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class OrderReferenceQuery {
    private final OrderRepository repository;
    public OrderReferenceQuery(OrderRepository repository) { this.repository = repository; }
    public boolean references(UUID customerId) { return repository.existsByCustomerId(customerId); }
}
