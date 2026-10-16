package com.example.agenticorderservice.order.api;
import java.util.List;
import java.util.UUID;
public record CreateOrderRequest(UUID customerId, List<OrderItemRequest> items) {
    public CreateOrderRequest { items = List.copyOf(items); }
}
