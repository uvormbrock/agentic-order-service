package com.example.agenticorderservice.customer.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.customer.application.port.out.*;
import com.example.agenticorderservice.customer.domain.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CustomerDeletionTest {
    @ParameterizedTest
    @CsvSource({"false,true", "false,false", "true,true", "true,false"})
    void locksBeforeReferenceQueryAndDeletesOrRejects(boolean referenced, boolean active) {
        var repository = mock(CustomerWriteStore.class);
        var orders = mock(CustomerOrderReferences.class);
        var service = new CustomerApplicationService(mock(CustomerReadStore.class), repository, orders);
        var customer = new Customer("C", new EmailAddress("a@example.com"));
        if (!active) customer.deactivate();
        UUID id = UUID.randomUUID();
        when(repository.findLockedById(id)).thenReturn(Optional.of(customer));
        when(orders.references(id)).thenReturn(referenced);
        if (referenced)
            assertThatExceptionOfType(CustomerHasOrdersException.class).isThrownBy(() -> service.delete(id));
        else service.delete(id);
        assertThat(customer.isActive()).isEqualTo(active);
        assertThat(customer.getName()).isEqualTo("C");
        assertThat(customer.getEmail().value()).isEqualTo("a@example.com");
        var ordered = inOrder(repository, orders);
        ordered.verify(repository).findLockedById(id);
        ordered.verify(orders).references(id);
        if (referenced) {
            verify(repository, never()).delete(any());
        } else {
            verify(repository).delete(customer);
        }
    }

    @Test
    void absentCustomerDoesNotQueryReferences() {
        var repository = mock(CustomerWriteStore.class);
        var orders = mock(CustomerOrderReferences.class);
        var service = new CustomerApplicationService(mock(CustomerReadStore.class), repository, orders);
        UUID missingId = UUID.randomUUID();
        assertThatExceptionOfType(CustomerNotFoundException.class).isThrownBy(() -> service.delete(missingId));
        verifyNoInteractions(orders);
    }

    @ParameterizedTest
    @CsvSource({
        "false,true,CUSTOMER_DELETED,COMMIT",
        "false,false,CUSTOMER_DELETED,COMMIT",
        "false,true,CUSTOMER_DELETED,ROLLBACK",
        "false,true,CUSTOMER_DELETED,UNKNOWN"
    })
    void independentSpringCompletionSequencesEmitOnlyAfterCommit(
            boolean referenced, boolean active, String event, String completion) {
        var repository = mock(CustomerWriteStore.class);
        var orders = mock(CustomerOrderReferences.class);
        var service = new CustomerApplicationService(mock(CustomerReadStore.class), repository, orders);
        UUID id = UUID.randomUUID();
        var customer = new Customer("C", new EmailAddress("secret@example.com"));
        if (!active) customer.deactivate();
        when(repository.findLockedById(id)).thenReturn(Optional.of(customer));
        when(orders.references(id)).thenReturn(referenced);
        var manager = new CompletionManager(completion.equals("UNKNOWN"));
        try (var capture =
                new com.example.agenticorderservice.shared.logging.EventCapture(CustomerApplicationService.class)) {
            Runnable invocation =
                    () -> new org.springframework.transaction.support.TransactionTemplate(manager).execute(status -> {
                        service.delete(id);
                        assertThat(capture.messages()).isEmpty();
                        if (completion.equals("ROLLBACK")) status.setRollbackOnly();
                        return null;
                    });
            if (completion.equals("UNKNOWN"))
                assertThatExceptionOfType(org.springframework.transaction.TransactionSystemException.class)
                        .isThrownBy(invocation::run);
            else invocation.run();
            if (completion.equals("COMMIT")) {
                assertThat(capture.messages()).containsExactly("event=" + event);
                assertThat(capture.events.getFirst().getLevel())
                        .isEqualTo(
                                event.equals("CUSTOMER_DELETE_NOOP")
                                        ? ch.qos.logback.classic.Level.DEBUG
                                        : ch.qos.logback.classic.Level.INFO);
            } else assertThat(capture.messages()).isEmpty();
            assertThat(capture.messages())
                    .allSatisfy(message -> assertThat(message).doesNotContain(id.toString(), "secret@example.com"));
            assertThat(
                            org.springframework.transaction.support.TransactionSynchronizationManager
                                    .isSynchronizationActive())
                    .isFalse();
        }
    }

    @Test
    void failingPostCommitAppenderPreservesSuccessfulTransactionResult() {
        var repository = mock(CustomerWriteStore.class);
        var orders = mock(CustomerOrderReferences.class);
        var service = new CustomerApplicationService(mock(CustomerReadStore.class), repository, orders);
        UUID id = UUID.randomUUID();
        when(repository.findLockedById(id))
                .thenReturn(Optional.of(new Customer("C", new EmailAddress("a@example.com"))));
        var manager = new CompletionManager(false);
        var logger =
                (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(CustomerApplicationService.class);
        var appender = new ch.qos.logback.core.AppenderBase<ch.qos.logback.classic.spi.ILoggingEvent>() {
            @Override
            public void doAppend(ch.qos.logback.classic.spi.ILoggingEvent event) {
                throw new IllegalStateException("appender");
            }

            @Override
            protected void append(ch.qos.logback.classic.spi.ILoggingEvent event) {
                // doAppend deliberately fails before this hook can be reached.
            }
        };
        appender.start();
        logger.addAppender(appender);
        try {
            String result = new org.springframework.transaction.support.TransactionTemplate(manager).execute(status -> {
                service.delete(id);
                return "committed";
            });
            assertThat(result).isEqualTo("committed");
            assertThat(manager.commits).isEqualTo(1);
            verify(repository).delete(any());
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    /** Exercises Spring synchronization sequencing, not a real database commit. */
    static class CompletionManager extends org.springframework.transaction.support.AbstractPlatformTransactionManager {
        final boolean failCommit;
        int commits;

        CompletionManager(boolean failCommit) {
            this.failCommit = failCommit;
        }

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, org.springframework.transaction.TransactionDefinition definition) {
            // Spring owns synchronization; this fixture has no physical transaction to begin.
        }

        @Override
        protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus status) {
            if (failCommit)
                throw new org.springframework.transaction.TransactionSystemException("unknown commit result");
            commits++;
        }

        @Override
        protected void doRollback(org.springframework.transaction.support.DefaultTransactionStatus status) {
            // Only synchronization sequencing is exercised; no database resources exist to roll back.
        }
    }
}
