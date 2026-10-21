package com.example.agenticorderservice.order.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Materialized stored order facts; amounts and item snapshots are never recalculated. */
public record OrderReadSnapshot(
        UUID id,
        UUID customerId,
        List<OrderResult.Item> items,
        String currency,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total) {
    public OrderReadSnapshot {
        items = List.copyOf(items);
    }
}
