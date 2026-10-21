package com.example.agenticorderservice.order.application.port.out;

import com.example.agenticorderservice.order.domain.Order;

/** Scoped same-feature aggregate access within the application-owned placement attempt. */
public interface OrderWriteStore {
    Order save(Order order);

    void flush();
}
