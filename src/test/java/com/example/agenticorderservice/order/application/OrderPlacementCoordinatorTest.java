package com.example.agenticorderservice.order.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.customer.application.*;
import com.example.agenticorderservice.order.persistence.OrderRepository;
import com.example.agenticorderservice.product.application.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

class OrderPlacementCoordinatorTest {
    final CustomerOrderAccess customers = mock(CustomerOrderAccess.class);
    final ProductOrderAccess products = mock(ProductOrderAccess.class);
    final OrderRepository orders = mock(OrderRepository.class);
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final Manager manager = new Manager();
    final AtomicLong clock = new AtomicLong(100000000000L);
    final List<Long> delays = new ArrayList<>();
    final UUID customer = UUID.randomUUID(),
            first = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            second = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    final OrderPlacementCoordinator coordinator = new OrderPlacementCoordinator(
            new TransactionTemplate(manager),
            jdbc,
            new AmbientTransactionGuard(),
            customers,
            products,
            orders,
            clock::get,
            delay -> {
                delays.add(delay);
                clock.addAndGet(delay);
            });

    @BeforeEach
    void facts() {
        when(customers.lock(customer)).thenReturn(new CustomerResult(customer, "Customer", "a@example.com", true));
        when(products.lock(any()))
                .thenAnswer(c -> new ProductResult(c.getArgument(0), "Original", new BigDecimal("25.00"), true));
        when(orders.save(any())).thenAnswer(c -> c.getArgument(0));
    }

    PlaceOrderCommand command() {
        return new PlaceOrderCommand(customer, List.of(new PlaceOrderCommand.Line(first, 4)));
    }

    @Test
    void consolidatesLocksCanonicalOrderAndCalculatesAcceptedThreshold() {
        var result = coordinator.place(new PlaceOrderCommand(
                customer,
                List.of(
                        new PlaceOrderCommand.Line(second, 1),
                        new PlaceOrderCommand.Line(first, 2),
                        new PlaceOrderCommand.Line(first, 1))));
        var order = inOrder(customers, products, orders);
        order.verify(customers).lock(customer);
        order.verify(products).lock(first);
        order.verify(products).lock(second);
        order.verify(orders).save(any());
        order.verify(orders).flush();
        verify(jdbc).execute("SET LOCAL lock_timeout = '2s'");
        assertThat(result.subtotal()).isEqualByComparingTo("100.00");
        assertThat(result.discount()).isEqualByComparingTo("10.00");
        assertThat(result.total()).isEqualByComparingTo("90.00");
        assertThat(result.items()).extracting(OrderResult.Item::quantity).containsExactly(3, 1);
        var immutableItems = result.items();
        assertThatThrownBy(immutableItems::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThat(manager.commits).isEqualTo(1);
        assertThat(manager.rollbacks).isZero();
    }

    @Test
    void retriesOnlyAfterRollbackAndReleasedTransactionWithApprovedBackoff() {
        when(products.lock(first))
                .thenThrow(new RuntimeException(new SQLException("deadlock", "40P01")))
                .thenThrow(new RuntimeException(new SQLException("serialization", "40001")))
                .thenReturn(new ProductResult(first, "Original", new BigDecimal("25.00"), true));
        coordinator.place(command());
        assertThat(manager.rollbacks).isEqualTo(2);
        assertThat(manager.commits).isEqualTo(1);
        assertThat(delays).containsExactly(25000000L, 50000000L);
        verify(orders).save(any());
        assertThat(manager.begins).isEqualTo(3);
    }

    @Test
    void exhaustionReturnsUnavailableAndNeverSaves() {
        when(products.lock(first))
                .thenThrow(new RuntimeException(OrderRetryPolicyTest.serverFailure("55P03", "ProcessInterrupts")));
        var input = command();
        assertThatExceptionOfType(OrderFailure.class)
                .isThrownBy(() -> coordinator.place(input))
                .satisfies(f -> assertThat(f.reason()).isEqualTo(OrderFailure.Reason.UNAVAILABLE));
        assertThat(manager.rollbacks).isEqualTo(3);
        verifyNoInteractions(orders);
    }

    @ParameterizedTest
    @ValueSource(longs = {9975000000L, 10000000000L})
    void schedulingBudgetPreventsAnotherAttempt(long elapsed) {
        when(products.lock(first)).thenAnswer(c -> {
            clock.set(100000000000L + elapsed);
            throw new RuntimeException(new SQLException("deadlock", "40P01"));
        });
        var input = command();
        assertThatExceptionOfType(OrderFailure.class)
                .isThrownBy(() -> coordinator.place(input))
                .satisfies(f -> assertThat(f.reason()).isEqualTo(OrderFailure.Reason.UNAVAILABLE));
        assertThat(manager.begins).isEqualTo(1);
        assertThat(delays).isEmpty();
    }

    @Test
    void committedButUnconfirmedOutcomeNeverReplays() {
        manager.failCommit = true;
        var input = command();
        assertThatExceptionOfType(OrderFailure.class)
                .isThrownBy(() -> coordinator.place(input))
                .satisfies(f -> assertThat(f.reason()).isEqualTo(OrderFailure.Reason.TECHNICAL));
        verify(orders).save(any());
        assertThat(manager.begins).isEqualTo(1);
        assertThat(delays).isEmpty();
    }

    @Test
    void inactiveCustomerHasPriorityAndNeverLocksProducts() {
        when(customers.lock(customer)).thenReturn(new CustomerResult(customer, "C", "a@example.com", false));
        var input = command();
        assertThatExceptionOfType(OrderFailure.class)
                .isThrownBy(() -> coordinator.place(input))
                .satisfies(f -> assertThat(f.reason()).isEqualTo(OrderFailure.Reason.CUSTOMER_INACTIVE));
        verifyNoInteractions(products, orders);
    }

    @ParameterizedTest
    @ValueSource(strings = {"40P01", "40001", "55P03", "57014", "23505", "08006", "HY000"})
    void classifierRequiresKnownStateAndLockOrigin(String state) {
        var failure = new RuntimeException(new SQLException("private", state));
        assertThat(OrderRetryPolicy.transientFailure(failure, false))
                .isEqualTo(Set.of("40P01", "40001").contains(state));
        assertThat(OrderRetryPolicy.transientFailure(failure, true))
                .isEqualTo(Set.of("40P01", "40001").contains(state));
        assertThat(OrderRetryPolicy.transientFailure(
                        new jakarta.persistence.OptimisticLockException("conflict", failure), true))
                .isFalse();
    }

    @Test
    void guardRejectsActiveSynchronizationAndBoundResourcesBeforeAnyProcessing() {
        Object key = new Object();
        TransactionSynchronizationManager.bindResource(key, new Object());
        var input = command();
        try {
            assertThatIllegalStateException().isThrownBy(() -> coordinator.place(input));
        } finally {
            TransactionSynchronizationManager.unbindResource(key);
        }
        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThatIllegalStateException().isThrownBy(() -> coordinator.place(input));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
        assertThat(manager.begins).isZero();
        verifyNoInteractions(customers, products, orders, jdbc);
    }

    @Test
    void missingCustomerIsNotRetriedAndNeverTouchesProducts() {
        when(customers.lock(customer)).thenThrow(new CustomerNotFoundException());
        var input = command();
        assertThatExceptionOfType(CustomerNotFoundException.class).isThrownBy(() -> coordinator.place(input));
        assertThat(manager.begins).isEqualTo(1);
        assertThat(manager.rollbacks).isEqualTo(1);
        verifyNoInteractions(products, orders);
        assertThat(delays).isEmpty();
    }

    @Test
    void missingProductIsNotRetriedOrSaved() {
        when(products.lock(first)).thenThrow(new ProductNotFoundException());
        var input = command();
        assertThatExceptionOfType(ProductNotFoundException.class).isThrownBy(() -> coordinator.place(input));
        assertThat(manager.begins).isEqualTo(1);
        verifyNoInteractions(orders);
        assertThat(delays).isEmpty();
    }

    @Test
    void inactiveProductRejectsBeforeDomainPersistence() {
        when(products.lock(first)).thenReturn(new ProductResult(first, "Inactive", BigDecimal.ZERO, false));
        var input = command();
        assertThatExceptionOfType(OrderFailure.class)
                .isThrownBy(() -> coordinator.place(input))
                .satisfies(f -> assertThat(f.reason()).isEqualTo(OrderFailure.Reason.PRODUCT_INACTIVE));
        verifyNoInteractions(orders);
        assertThat(manager.rollbacks).isEqualTo(1);
        assertThat(delays).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"57014", "23505", "08006", "HY000"})
    void rejectedSqlFailuresRollBackExactlyOnceWithoutReplay(String state) {
        var failure = new RuntimeException(new SQLException("private", state));
        when(products.lock(first)).thenThrow(failure);
        var input = command();
        assertThatThrownBy(() -> coordinator.place(input)).isSameAs(failure);
        assertThat(manager.begins).isEqualTo(1);
        assertThat(manager.rollbacks).isEqualTo(1);
        verifyNoInteractions(orders);
        assertThat(delays).isEmpty();
    }

    @Test
    void budgetIsCheckedAgainAfterBackoffWithoutBeginningAnotherAttempt() {
        when(products.lock(first)).thenThrow(new RuntimeException(new SQLException("deadlock", "40P01")));
        var slowPause = new OrderPlacementCoordinator(
                new TransactionTemplate(manager),
                jdbc,
                new AmbientTransactionGuard(),
                customers,
                products,
                orders,
                clock::get,
                delay -> clock.set(110000000000L));
        var input = command();
        assertThatExceptionOfType(OrderFailure.class)
                .isThrownBy(() -> slowPause.place(input))
                .satisfies(f -> assertThat(f.reason()).isEqualTo(OrderFailure.Reason.UNAVAILABLE));
        assertThat(manager.begins).isEqualTo(1);
        verifyNoInteractions(orders);
    }

    @Test
    void interruptedRealBackoffPreservesInterruptAndNeverBeginsAnotherAttempt() {
        when(products.lock(first)).thenThrow(new RuntimeException(new SQLException("deadlock", "40P01")));
        var realPause = new OrderPlacementCoordinator(
                new TransactionTemplate(manager), jdbc, new AmbientTransactionGuard(), customers, products, orders);
        Thread.currentThread().interrupt();
        try {
            var input = command();
            assertThatExceptionOfType(OrderFailure.class)
                    .isThrownBy(() -> realPause.place(input))
                    .satisfies(f -> {
                        assertThat(f.reason()).isEqualTo(OrderFailure.Reason.TECHNICAL);
                        assertThat(f.getCause()).isInstanceOf(InterruptedException.class);
                    });
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            assertThat(manager.begins).isEqualTo(1);
            verifyNoInteractions(orders);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void leakedRollbackResourceStopsRetryBeforeBackoff() {
        manager.leakResource = true;
        when(products.lock(first)).thenThrow(new RuntimeException(new SQLException("deadlock", "40P01")));
        try {
            var input = command();
            assertThatIllegalStateException().isThrownBy(() -> coordinator.place(input));
            assertThat(manager.begins).isEqualTo(1);
            assertThat(delays).isEmpty();
            verifyNoInteractions(orders);
        } finally {
            TransactionSynchronizationManager.unbindResourceIfPossible(manager);
        }
    }

    @Test
    void unrelatedNested55P03FromProviderDoesNotReplay() {
        var failure = new RuntimeException(new SQLException("not attributed", "55P03"));
        when(products.lock(first)).thenThrow(failure);
        var input = command();
        assertThatThrownBy(() -> coordinator.place(input)).isSameAs(failure);
        assertThat(manager.begins).isEqualTo(1);
        assertThat(manager.rollbacks).isEqualTo(1);
        assertThat(delays).isEmpty();
        verifyNoInteractions(orders);
    }

    @Test
    void loggingObservesConfirmedCompletionAndRetrySchedulingWithoutPrivateData() {
        try (var capture =
                new com.example.agenticorderservice.shared.logging.EventCapture(OrderPlacementCoordinator.class)) {
            doAnswer(call -> {
                        assertThat(capture.messages()).noneMatch(message -> message.contains("ORDER_COMMITTED"));
                        return null;
                    })
                    .when(orders)
                    .flush();
            when(products.lock(first))
                    .thenThrow(new RuntimeException(new SQLException("secret-canary", "40P01")))
                    .thenReturn(new ProductResult(first, "private-name", new BigDecimal("25"), true));
            coordinator.place(command());
            assertThat(capture.messages()).hasSize(3);
            assertThat(capture.messages().get(0)).contains("ORDER_ATTEMPT_ROLLED_BACK");
            assertThat(capture.messages().get(1))
                    .contains("ORDER_RETRY_SCHEDULED", "attempt=1", "nextAttempt=2", "delayMs=25");
            assertThat(capture.messages().get(2)).contains("ORDER_COMMITTED", "attempt=2");
            assertThat(capture.messages())
                    .allSatisfy(message -> assertThat(message)
                            .doesNotContain("secret-canary", "private-name", customer.toString(), first.toString()));
        }
    }

    @Test
    void unknownCompletionLogsOnlyUnknownAndMarksDiagnosticOwnership() {
        manager.failCommit = true;
        try (var capture =
                new com.example.agenticorderservice.shared.logging.EventCapture(OrderPlacementCoordinator.class)) {
            assertThatExceptionOfType(OrderFailure.class)
                    .isThrownBy(() -> coordinator.place(command()))
                    .satisfies(failure -> assertThat(failure.diagnosticOwned()).isTrue());
            assertThat(capture.messages()).singleElement().asString().contains("ORDER_OUTCOME_UNKNOWN");
        }
    }

    @Test
    void failingAppenderCannotConvertCommittedOrderIntoReplay() {
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(OrderPlacementCoordinator.class);
        var appender = new ch.qos.logback.core.AppenderBase<ch.qos.logback.classic.spi.ILoggingEvent>() {
            @Override
            public void doAppend(ch.qos.logback.classic.spi.ILoggingEvent event) {
                throw new IllegalStateException("appender unavailable");
            }

            @Override
            protected void append(ch.qos.logback.classic.spi.ILoggingEvent event) {}
        };
        appender.start();
        logger.addAppender(appender);
        try {
            assertThat(coordinator.place(command()).total()).isEqualByComparingTo("90");
            assertThat(manager.commits).isEqualTo(1);
            assertThat(manager.begins).isEqualTo(1);
            assertThat(delays).isEmpty();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"CUSTOMER_NOT_FOUND", "PRODUCT_NOT_FOUND", "CUSTOMER_INACTIVE", "PRODUCT_INACTIVE"})
    void rejectionEventsUseFixedSafeReasons(String reason) {
        if (reason.equals("CUSTOMER_NOT_FOUND"))
            when(customers.lock(customer)).thenThrow(new CustomerNotFoundException());
        else if (reason.equals("PRODUCT_NOT_FOUND"))
            when(products.lock(first)).thenThrow(new ProductNotFoundException());
        else if (reason.equals("CUSTOMER_INACTIVE"))
            when(customers.lock(customer))
                    .thenReturn(new CustomerResult(customer, "private", "secret@example.com", false));
        else when(products.lock(first)).thenReturn(new ProductResult(first, "private", BigDecimal.ONE, false));
        try (var capture =
                new com.example.agenticorderservice.shared.logging.EventCapture(OrderPlacementCoordinator.class)) {
            assertThatThrownBy(() -> coordinator.place(command())).isInstanceOf(RuntimeException.class);
            assertThat(capture.messages()).containsExactly("event=ORDER_REJECTED attempt=1 reason=" + reason);
            assertThat(capture.events.getFirst().getLevel()).isEqualTo(ch.qos.logback.classic.Level.DEBUG);
        }
    }

    @Test
    void retryTransitionsExhaustionAndCommitBoundaryHaveExplicitFields() {
        when(products.lock(first)).thenThrow(new RuntimeException(new SQLException("secret-canary", "40P01")));
        try (var capture =
                new com.example.agenticorderservice.shared.logging.EventCapture(OrderPlacementCoordinator.class)) {
            assertThatExceptionOfType(OrderFailure.class).isThrownBy(() -> coordinator.place(command()));
            assertThat(capture.messages())
                    .contains(
                            "event=ORDER_RETRY_SCHEDULED attempt=1 nextAttempt=2 delayMs=25",
                            "event=ORDER_RETRY_SCHEDULED attempt=2 nextAttempt=3 delayMs=50",
                            "event=ORDER_RETRY_EXHAUSTED attempts=3 reason=ATTEMPT_LIMIT");
            assertThat(capture.messages().stream()
                            .filter(m -> m.contains("ORDER_RETRY_SCHEDULED"))
                            .count())
                    .isEqualTo(2);
            assertThat(capture.messages())
                    .noneMatch(m ->
                            m.contains("secret-canary") || m.contains("countOrDelay") || m.contains("ORDER_COMMITTED"));
        }
    }

    @Test
    void budgetAndInterruptedBackoffHaveDistinctExhaustionReasons() {
        when(products.lock(first)).thenThrow(new RuntimeException(new SQLException("private", "40P01")));
        try (var capture =
                new com.example.agenticorderservice.shared.logging.EventCapture(OrderPlacementCoordinator.class)) {
            var budget = new OrderPlacementCoordinator(
                    new TransactionTemplate(manager),
                    jdbc,
                    new AmbientTransactionGuard(),
                    customers,
                    products,
                    orders,
                    clock::get,
                    delay -> clock.addAndGet(TimeUnit.SECONDS.toNanos(10)));
            assertThatExceptionOfType(OrderFailure.class).isThrownBy(() -> budget.place(command()));
            assertThat(capture.messages()).contains("event=ORDER_RETRY_EXHAUSTED attempts=1 reason=SCHEDULING_BUDGET");
            capture.events.clear();
            var interrupted = new OrderPlacementCoordinator(
                    new TransactionTemplate(manager),
                    jdbc,
                    new AmbientTransactionGuard(),
                    customers,
                    products,
                    orders,
                    clock::get,
                    delay -> {
                        throw new OrderFailure(OrderFailure.Reason.TECHNICAL, new InterruptedException("private"));
                    });
            assertThatExceptionOfType(OrderFailure.class).isThrownBy(() -> interrupted.place(command()));
            assertThat(capture.messages())
                    .contains("event=ORDER_RETRY_EXHAUSTED attempts=1 reason=BACKOFF_INTERRUPTED");
        }
    }

    @Test
    void commitCheckpointPrecedesSuccessEmission() {
        try (var capture =
                new com.example.agenticorderservice.shared.logging.EventCapture(OrderPlacementCoordinator.class)) {
            manager.beforeCommit = () -> assertThat(capture.messages()).noneMatch(m -> m.contains("ORDER_COMMITTED"));
            coordinator.place(command());
            assertThat(capture.messages()).containsExactly("event=ORDER_COMMITTED attempt=1 consolidatedItemCount=1");
            assertThat(capture.events.getFirst().getLevel()).isEqualTo(ch.qos.logback.classic.Level.INFO);
        }
    }

    static class Manager extends AbstractPlatformTransactionManager {
        int begins, commits, rollbacks;
        boolean failCommit, leakResource;
        Runnable beforeCommit = () -> {};

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
            begins++;
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            beforeCommit.run();
            commits++;
            if (failCommit) {
                throw new TransactionSystemException("confirmation lost");
            }
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbacks++;
            if (leakResource) TransactionSynchronizationManager.bindResource(this, new Object());
        }
    }
}
