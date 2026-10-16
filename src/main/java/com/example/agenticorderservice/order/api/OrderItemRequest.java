package com.example.agenticorderservice.order.api;
import java.util.UUID;
public record OrderItemRequest(UUID productId, Integer quantity) { }
