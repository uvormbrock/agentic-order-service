package com.example.agenticorderservice.order.application.port.out;

import com.example.agenticorderservice.order.application.OrderReadSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderReadStore {
    Optional<OrderReadSnapshot> findById(UUID id);

    List<OrderReadSnapshot> findAll();
}
