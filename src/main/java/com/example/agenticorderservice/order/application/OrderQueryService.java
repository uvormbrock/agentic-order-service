package com.example.agenticorderservice.order.application;

import com.example.agenticorderservice.customer.application.CustomerOrderAccess;
import com.example.agenticorderservice.order.persistence.OrderRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderQueryService {
    private final OrderRepository orders;
    private final CustomerOrderAccess customers;

    public OrderQueryService(OrderRepository orders, CustomerOrderAccess customers) {
        this.orders = orders;
        this.customers = customers;
    }

    public OrderResult get(UUID id) {
        var order = orders.findByIdWithItems(id).orElseThrow(() -> new OrderFailure(OrderFailure.Reason.NOT_FOUND));
        return OrderResult.from(
                order, customers.current(Set.of(order.getCustomerId())).get(order.getCustomerId()));
    }

    public List<OrderResult> list() {
        var roots = orders.findAllWithItems();
        var ids = roots.stream()
                .map(com.example.agenticorderservice.order.domain.Order::getCustomerId)
                .collect(java.util.stream.Collectors.toSet());
        var current = customers.current(ids);
        return roots.stream()
                .map(order -> OrderResult.from(order, current.get(order.getCustomerId())))
                .toList();
    }
}
