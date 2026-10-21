package com.example.agenticorderservice.order.application.port.in;

import com.example.agenticorderservice.order.application.OrderResult;
import java.util.List;
import java.util.UUID;

public interface OrderQueries {
    OrderResult get(UUID id);

    List<OrderResult> list();
}
