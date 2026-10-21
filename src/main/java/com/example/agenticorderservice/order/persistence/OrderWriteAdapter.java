package com.example.agenticorderservice.order.persistence;

import com.example.agenticorderservice.order.application.port.out.OrderWriteStore;
import com.example.agenticorderservice.order.domain.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderWriteAdapter implements OrderWriteStore {
    private final OrderRepository orders;

    public OrderWriteAdapter(OrderRepository orders) {
        this.orders = orders;
    }

    @Override
    public Order save(Order order) {
        return orders.save(order);
    }

    @Override
    public void flush() {
        orders.flush();
    }
}
