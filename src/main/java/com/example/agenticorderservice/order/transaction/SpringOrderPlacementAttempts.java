package com.example.agenticorderservice.order.transaction;

import com.example.agenticorderservice.order.application.OrderResult;
import com.example.agenticorderservice.order.application.PlacementAttempt;
import com.example.agenticorderservice.order.application.PlacementAttempt.Completion;
import com.example.agenticorderservice.order.application.port.out.OrderPlacementAttempts;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class SpringOrderPlacementAttempts implements OrderPlacementAttempts {
    private final TransactionTemplate transactions;
    private final JdbcTemplate jdbc;
    private final AmbientTransactionGuard guard;

    public SpringOrderPlacementAttempts(
            @Qualifier("orderPlacementTransactionTemplate") TransactionTemplate transactions,
            JdbcTemplate jdbc,
            AmbientTransactionGuard guard) {
        this.transactions = transactions;
        this.jdbc = jdbc;
        this.guard = guard;
    }

    @Override
    public void checkUnbound() {
        guard.check();
    }

    @Override
    public OrderResult execute(PlacementAttempt attempt, Supplier<OrderResult> work) {
        // execute() returns only after transaction completion and resource cleanup.
        return transactions.execute(status -> {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int completion) {
                    attempt.complete(
                            switch (completion) {
                                case TransactionSynchronization.STATUS_COMMITTED -> Completion.COMMITTED;
                                case TransactionSynchronization.STATUS_ROLLED_BACK -> Completion.ROLLED_BACK;
                                default -> Completion.UNKNOWN;
                            });
                }
            });
            jdbc.execute("SET LOCAL lock_timeout = '2s'");
            return work.get();
        });
    }

    @Override
    public boolean transientFailure(RuntimeException failure, boolean originalLockCallFailed) {
        return OrderRetryPolicy.transientFailure(failure, originalLockCallFailed);
    }
}
