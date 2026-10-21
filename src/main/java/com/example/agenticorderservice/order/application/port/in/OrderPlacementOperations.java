package com.example.agenticorderservice.order.application.port.in;

import com.example.agenticorderservice.order.application.OrderResult;
import com.example.agenticorderservice.order.application.PlaceOrderCommand;

public interface OrderPlacementOperations {
    OrderResult place(PlaceOrderCommand command);
}
