package com.example.agenticorderservice.order.persistence;

import com.example.agenticorderservice.order.application.OrderReadSnapshot;
import com.example.agenticorderservice.order.application.OrderResult;
import com.example.agenticorderservice.order.application.port.out.OrderReadStore;
import com.example.agenticorderservice.order.application.port.out.OrderReferenceStore;
import com.example.agenticorderservice.order.domain.Order;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrderReadAdapter implements OrderReadStore, OrderReferenceStore {
    private final OrderRepository orders;

    public OrderReadAdapter(OrderRepository orders) {
        this.orders = orders;
    }

    @Override
    public Optional<OrderReadSnapshot> findById(UUID id) {
        return orders.findByIdWithItems(id).map(this::snapshot);
    }

    @Override
    public List<OrderReadSnapshot> findAll() {
        return orders.findAllWithItems().stream().map(this::snapshot).toList();
    }

    private OrderReadSnapshot snapshot(Order order) {
        return new OrderReadSnapshot(
                order.getId(),
                order.getCustomerId(),
                order.getItems().stream()
                        .map(item -> new OrderResult.Item(
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

    @Override
    public boolean references(UUID customerId) {
        return orders.existsByCustomerId(customerId);
    }
}
