package com.example.agenticorderservice.order.application;

import com.example.agenticorderservice.customer.application.CustomerResult;
import com.example.agenticorderservice.order.application.port.in.OrderQueries;
import com.example.agenticorderservice.order.application.port.out.OrderCustomerFacts;
import com.example.agenticorderservice.order.application.port.out.OrderReadStore;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderQueryService implements OrderQueries {
    private final OrderReadStore orders;
    private final OrderCustomerFacts customers;

    public OrderQueryService(OrderReadStore orders, OrderCustomerFacts customers) {
        this.orders = orders;
        this.customers = customers;
    }

    @Override
    public OrderResult get(UUID id) {
        var order = orders.findById(id).orElseThrow(() -> new OrderFailure(OrderFailure.Reason.NOT_FOUND));
        return result(order, customers.current(Set.of(order.customerId())).get(order.customerId()));
    }

    @Override
    public List<OrderResult> list() {
        var roots = orders.findAll();
        var ids = roots.stream().map(OrderReadSnapshot::customerId).collect(java.util.stream.Collectors.toSet());
        var current = customers.current(ids);
        return roots.stream()
                .map(order -> result(order, current.get(order.customerId())))
                .toList();
    }

    private OrderResult result(OrderReadSnapshot order, CustomerResult customer) {
        if (customer == null) {
            throw new IllegalStateException("Referenced customer is missing");
        }
        return new OrderResult(
                order.id(),
                order.customerId(),
                customer,
                order.items(),
                order.currency(),
                order.subtotal(),
                order.discount(),
                order.total());
    }
}
