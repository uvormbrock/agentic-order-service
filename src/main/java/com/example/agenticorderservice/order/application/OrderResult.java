package com.example.agenticorderservice.order.application;

import com.example.agenticorderservice.customer.application.CustomerResult;
import com.example.agenticorderservice.order.domain.Order;
import java.math.BigDecimal;
import java.util.*;

public record OrderResult(
        UUID id,
        UUID customerId,
        CustomerResult customer,
        List<Item> items,
        String currency,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total) {
    public record Item(UUID productId, String productName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}

    public OrderResult {
        items = List.copyOf(items);
    }

    static OrderResult from(Order order, CustomerResult customer) {
        if (customer == null) {
            throw new IllegalStateException("Referenced customer is missing");
        }
        return new OrderResult(
                order.getId(),
                order.getCustomerId(),
                customer,
                order.getItems().stream()
                        .map(item -> new Item(
                                item.getProductId(),
                                item.getProductName(),
                                item.getQuantity(),
                                item.getUnitPrice().amount(),
                                item.getLineTotal().amount()))
                        .toList(),
                "EUR",
                order.getSubtotal().amount(),
                order.getDiscount().amount(),
                order.getTotal().amount());
    }
}
