package com.example.agenticorderservice.product.storage;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.*;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class SpringProductImageTransactionsTest {
    final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    final SpringProductImageTransactions transactions = new SpringProductImageTransactions(manager);

    @Test
    void returnsMaterializedResultOnlyAfterReadCommittedTransactionCommit() {
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        @SuppressWarnings("unchecked")
        Supplier<String> operation = mock(Supplier.class);
        when(operation.get()).thenReturn("materialized");
        assertThat(transactions.execute(operation)).isEqualTo("materialized");
        var order = inOrder(manager, operation);
        order.verify(manager)
                .getTransaction(argThat(
                        definition -> definition.getIsolationLevel() == TransactionDefinition.ISOLATION_READ_COMMITTED
                                && definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED));
        order.verify(operation).get();
        order.verify(manager).commit(any());
    }

    @Test
    void propagatesOriginalOperationFailureAfterRollbackWithoutReplay() {
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        var original = new IllegalArgumentException("operation failed");
        @SuppressWarnings("unchecked")
        Supplier<Object> operation = mock(Supplier.class);
        when(operation.get()).thenThrow(original);
        assertThatThrownBy(() -> transactions.execute(operation)).isSameAs(original);
        verify(operation).get();
        verify(manager).rollback(any());
        verify(manager, never()).commit(any());
    }

    @Test
    void preservesUnknownCommitAndUnexpectedRollbackFailuresWithoutReplay() {
        for (RuntimeException failure : new RuntimeException[] {
            new TransactionSystemException("uncertain outcome"), new UnexpectedRollbackException("rolled back")
        }) {
            reset(manager);
            when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
            doThrow(failure).when(manager).commit(any());
            @SuppressWarnings("unchecked")
            Supplier<String> operation = mock(Supplier.class);
            when(operation.get()).thenReturn("candidate");
            assertThatThrownBy(() -> transactions.execute(operation)).isSameAs(failure);
            verify(operation).get();
            verify(manager).commit(any());
            verify(manager, never()).rollback(any());
        }
    }

    @Test
    void ambientActualTransactionSynchronizationAndBoundResourcesAreEachRejected() {
        transactions.requireNoAmbientTransaction();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            assertThatIllegalStateException().isThrownBy(transactions::requireNoAmbientTransaction);
        } finally {
            TransactionSynchronizationManager.clear();
        }
        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThatIllegalStateException().isThrownBy(transactions::requireNoAmbientTransaction);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
        var key = new Object();
        TransactionSynchronizationManager.bindResource(key, new Object());
        try {
            assertThatIllegalStateException().isThrownBy(transactions::requireNoAmbientTransaction);
        } finally {
            TransactionSynchronizationManager.unbindResource(key);
        }
        transactions.requireNoAmbientTransaction();
        verifyNoInteractions(manager);
    }
}
