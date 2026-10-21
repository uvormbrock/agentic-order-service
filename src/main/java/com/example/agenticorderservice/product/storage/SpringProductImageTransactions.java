package com.example.agenticorderservice.product.storage;

import com.example.agenticorderservice.product.application.port.out.ProductImageTransactions;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class SpringProductImageTransactions implements ProductImageTransactions {
    private final TransactionTemplate transaction;

    public SpringProductImageTransactions(PlatformTransactionManager manager) {
        transaction = new TransactionTemplate(manager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    @Override
    public void requireNoAmbientTransaction() {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.getResourceMap().isEmpty())
            throw new IllegalStateException("Product images require no ambient transaction or bound resources");
    }

    @Override
    public <T> T execute(Supplier<T> operation) {
        return transaction.execute(status -> operation.get());
    }
}
