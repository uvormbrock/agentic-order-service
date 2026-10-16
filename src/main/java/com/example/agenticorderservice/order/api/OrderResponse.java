package com.example.agenticorderservice.order.api;

import com.example.agenticorderservice.customer.application.CustomerResult;
import com.example.agenticorderservice.order.application.OrderResult;
import java.math.BigDecimal;
import java.util.*;

public record OrderResponse(
        UUID id,
        UUID customerId,
        CustomerResult customer,
        List<OrderResult.Item> items,
        String currency,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total) {
    public OrderResponse {
        items = List.copyOf(items);
    }

    static OrderResponse from(OrderResult value) {
        return new OrderResponse(
                value.id(),
                value.customerId(),
                value.customer(),
                value.items(),
                value.currency(),
                value.subtotal(),
                value.discount(),
                value.total());
    }
}
