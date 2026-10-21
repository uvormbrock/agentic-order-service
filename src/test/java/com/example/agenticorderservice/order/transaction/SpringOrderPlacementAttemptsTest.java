package com.example.agenticorderservice.order.transaction;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.order.application.*;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

class SpringOrderPlacementAttemptsTest {
    final Manager manager = new Manager();
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final SpringOrderPlacementAttempts adapter =
            new SpringOrderPlacementAttempts(new TransactionTemplate(manager), jdbc, new AmbientTransactionGuard());

    @Test
    void registersCompletionBeforeLocalTimeoutAndReturnsOnlyAfterCommitCleanup() {
        var attempt = new PlacementAttempt();
        var result = mock(OrderResult.class);
        doAnswer(call -> {
                    assertThat(TransactionSynchronizationManager.getSynchronizations())
                            .hasSize(1);
                    assertThat(attempt.completion()).isEqualTo(PlacementAttempt.Completion.UNKNOWN);
                    return null;
                })
                .when(jdbc)
                .execute("SET LOCAL lock_timeout = '2s'");
        assertThat(adapter.execute(attempt, () -> {
                    verify(jdbc).execute("SET LOCAL lock_timeout = '2s'");
                    assertThat(attempt.completion()).isEqualTo(PlacementAttempt.Completion.UNKNOWN);
                    return result;
                }))
                .isSameAs(result);
        assertThat(manager.commits).isEqualTo(1);
        assertThat(attempt.completion()).isEqualTo(PlacementAttempt.Completion.COMMITTED);
        adapter.checkUnbound();
    }

    @Test
    void preservesCallbackFailureAndObservesConfirmedRollbackBeforeReturningControl() {
        var failure = new RuntimeException(new SQLException("eligible", "40P01"));
        var attempt = new PlacementAttempt();
        assertThatThrownBy(() -> adapter.execute(attempt, () -> {
                    throw failure;
                }))
                .isSameAs(failure);
        assertThat(manager.rollbacks).isEqualTo(1);
        assertThat(attempt.completion()).isEqualTo(PlacementAttempt.Completion.ROLLED_BACK);
        adapter.checkUnbound();
        assertThat(adapter.transientFailure(failure, false)).isTrue();
    }

    @Test
    void timeoutSetupFailureStillReportsRollbackWithoutCallingBusinessWork() {
        var failure = new IllegalStateException("setup");
        doThrow(failure).when(jdbc).execute("SET LOCAL lock_timeout = '2s'");
        var attempt = new PlacementAttempt();
        assertThatThrownBy(() -> adapter.execute(attempt, () -> {
                    throw new AssertionError("must not run");
                }))
                .isSameAs(failure);
        assertThat(attempt.completion()).isEqualTo(PlacementAttempt.Completion.ROLLED_BACK);
        adapter.checkUnbound();
    }

    @Test
    void failedCommitStaysUnknownAndFailureIdentityIsUnchanged() {
        var failure = new TransactionSystemException("confirmation lost");
        manager.commitFailure = failure;
        var attempt = new PlacementAttempt();
        assertThatThrownBy(() -> adapter.execute(attempt, () -> null)).isSameAs(failure);
        assertThat(attempt.completion()).isEqualTo(PlacementAttempt.Completion.UNKNOWN);
        assertThat(manager.rollbacks).isZero();
        adapter.checkUnbound();
        assertThat(adapter.transientFailure(failure, true)).isFalse();
    }

    @Test
    void absentCallbackAndTransactionCreationFailureLeaveCompletionUnknown() {
        var template = mock(TransactionTemplate.class);
        var selected = new SpringOrderPlacementAttempts(template, jdbc, new AmbientTransactionGuard());
        var attempt = new PlacementAttempt();
        assertThat(selected.execute(attempt, () -> {
                    throw new AssertionError("must not run");
                }))
                .isNull();
        assertThat(attempt.completion()).isEqualTo(PlacementAttempt.Completion.UNKNOWN);
        var failure = new CannotCreateTransactionException("begin failed");
        when(template.execute(any())).thenThrow(failure);
        assertThatThrownBy(() -> selected.execute(attempt, () -> {
                    throw new AssertionError("must not run");
                }))
                .isSameAs(failure);
        assertThat(attempt.completion()).isEqualTo(PlacementAttempt.Completion.UNKNOWN);
        verifyNoInteractions(jdbc);
    }

    @Test
    void unrecognizedCompletionFailsClosedToUnknown() {
        var template = mock(TransactionTemplate.class);
        when(template.execute(any())).thenAnswer(call -> {
            TransactionCallback<OrderResult> callback = call.getArgument(0);
            TransactionSynchronizationManager.initSynchronization();
            try {
                var result = callback.doInTransaction(mock(TransactionStatus.class));
                for (var synchronization : TransactionSynchronizationManager.getSynchronizations())
                    synchronization.afterCompletion(99);
                return result;
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        });
        var selected = new SpringOrderPlacementAttempts(template, jdbc, new AmbientTransactionGuard());
        var attempt = new PlacementAttempt();
        selected.execute(attempt, () -> null);
        assertThat(attempt.completion()).isEqualTo(PlacementAttempt.Completion.UNKNOWN);
        selected.checkUnbound();
    }

    static class Manager extends AbstractPlatformTransactionManager {
        int commits, rollbacks;
        RuntimeException commitFailure;

        protected Object doGetTransaction() {
            return new Object();
        }

        protected void doBegin(Object transaction, TransactionDefinition definition) {}

        protected void doCommit(DefaultTransactionStatus status) {
            commits++;
            if (commitFailure != null) throw commitFailure;
        }

        protected void doRollback(DefaultTransactionStatus status) {
            rollbacks++;
        }
    }
}
