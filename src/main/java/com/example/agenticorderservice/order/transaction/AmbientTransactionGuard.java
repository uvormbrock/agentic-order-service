package com.example.agenticorderservice.order.transaction;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class AmbientTransactionGuard {
    public void check() {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.getResourceMap().isEmpty()) {
            throw new IllegalStateException(
                    "Order placement requires entry without an ambient transaction or bound resources");
        }
    }
}
