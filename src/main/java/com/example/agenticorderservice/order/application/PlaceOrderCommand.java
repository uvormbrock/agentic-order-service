package com.example.agenticorderservice.order.application;

import java.util.*;
import static com.example.agenticorderservice.order.application.OrderFailure.Reason.*;

public record PlaceOrderCommand(UUID customerId, List<Line> items) {
    public record Line(UUID productId, Integer quantity) { }
    public PlaceOrderCommand {
        if (customerId == null || items == null) { throw new OrderFailure(REQUIRED); }
        if (items.isEmpty()) { throw new OrderFailure(ITEMS); }
        for (Line item : items) {
            if (item == null || item.productId() == null || item.quantity() == null) { throw new OrderFailure(REQUIRED); }
        }
        items = List.copyOf(items);
        quantities(items); // Also protects non-HTTP application callers before persistence access.
    }
    public Map<UUID, Integer> consolidated() { return quantities(items); }
    private static Map<UUID, Integer> quantities(List<Line> items) {
        Map<UUID, Integer> result = new TreeMap<>(Comparator.comparing(UUID::toString));
        for (Line item : items) {
            int quantity = item.quantity();
            if (quantity < 1 || quantity > 10000) { throw new OrderFailure(QUANTITY); }
            int sum = result.getOrDefault(item.productId(), 0) + quantity;
            // Every intermediate sum is at most 20,000; no narrowing/overflow is possible.
            if (sum > 10000) { throw new OrderFailure(QUANTITY); }
            result.put(item.productId(), sum);
        }
        return Collections.unmodifiableMap(result);
    }
}
