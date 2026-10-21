package com.example.agenticorderservice.order.application.port.out;

import com.example.agenticorderservice.order.application.OrderResult;
import com.example.agenticorderservice.order.application.PlacementAttempt;
import java.util.function.Supplier;

public interface OrderPlacementAttempts {
    void checkUnbound();

    OrderResult execute(PlacementAttempt attempt, Supplier<OrderResult> work);

    boolean transientFailure(RuntimeException failure, boolean originalLockCallFailed);
}
