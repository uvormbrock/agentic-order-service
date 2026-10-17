package com.example.agenticorderservice.order.application;

import static com.example.agenticorderservice.order.application.OrderFailure.Reason.*;

import com.example.agenticorderservice.customer.application.CustomerOrderAccess;
import com.example.agenticorderservice.order.domain.*;
import com.example.agenticorderservice.order.persistence.OrderRepository;
import com.example.agenticorderservice.product.application.ProductOrderAccess;
import com.example.agenticorderservice.shared.domain.Money;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.*;

@Service
public class OrderPlacementCoordinator {
    private final TransactionTemplate transactions;
    private final JdbcTemplate jdbc;
    private final AmbientTransactionGuard guard;
    private final CustomerOrderAccess customers;
    private final ProductOrderAccess products;
    private final OrderRepository orders;
    private final LongSupplier clock;
    private final LongConsumer pause;

    @Autowired
    public OrderPlacementCoordinator(
            @Qualifier("orderPlacementTransactionTemplate") TransactionTemplate transactions,
            JdbcTemplate jdbc,
            AmbientTransactionGuard guard,
            CustomerOrderAccess customers,
            ProductOrderAccess products,
            OrderRepository orders) {
        this(transactions, jdbc, guard, customers, products, orders, System::nanoTime, nanos -> {
            try {
                TimeUnit.NANOSECONDS.sleep(nanos);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new OrderFailure(TECHNICAL, interrupted);
            }
        });
    }

    OrderPlacementCoordinator(
            TransactionTemplate transactions,
            JdbcTemplate jdbc,
            AmbientTransactionGuard guard,
            CustomerOrderAccess customers,
            ProductOrderAccess products,
            OrderRepository orders,
            LongSupplier clock,
            LongConsumer pause) {
        this.transactions = transactions;
        this.jdbc = jdbc;
        this.guard = guard;
        this.customers = customers;
        this.products = products;
        this.orders = orders;
        this.clock = clock;
        this.pause = pause;
    }

    private static final class Attempt {
        int completion = TransactionSynchronization.STATUS_UNKNOWN;
        RuntimeException lockCallFailure;
    }

    public OrderResult place(PlaceOrderCommand command) {
        guard.check();
        Objects.requireNonNull(command, "command");
        var quantities = command.consolidated();
        long started = clock.getAsLong();
        for (int number = 0; number < 3; number++) {
            var attempt = new Attempt();
            try {
                var result = executeAttempt(command, quantities, attempt);
                event(
                        org.slf4j.event.Level.INFO,
                        "event=ORDER_COMMITTED attempt={} consolidatedItemCount={}",
                        number + 1,
                        quantities.size());
                return result;
            } catch (RuntimeException failure) {
                retryAfterRollback(failure, attempt, number, started);
            }
        }
        throw new IllegalStateException("Unreachable attempt boundary");
    }

    private OrderResult executeAttempt(PlaceOrderCommand command, Map<UUID, Integer> quantities, Attempt attempt) {
        // execute() returns only after transaction completion and resource cleanup.
        return transactions.execute(status -> {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int completion) {
                    attempt.completion = completion;
                }
            });
            jdbc.execute("SET LOCAL lock_timeout = '2s'");
            var customer = lockCustomer(command.customerId(), attempt);
            if (!customer.active()) {
                throw new OrderFailure(CUSTOMER_INACTIVE);
            }
            List<OrderLineInput> lines = new ArrayList<>();
            for (var entry : quantities.entrySet()) {
                var product = lockProduct(entry.getKey(), attempt);
                if (!product.active()) {
                    throw new OrderFailure(PRODUCT_INACTIVE);
                }
                lines.add(new OrderLineInput(
                        entry.getKey(), product.name(), new Money(product.price()), entry.getValue()));
            }
            var order = orders.save(new Order(command.customerId(), lines));
            orders.flush(); // Surface aborts inside the attempt; commit is a separate completion boundary.
            return OrderResult.from(order, customer);
        });
    }

    private com.example.agenticorderservice.customer.application.CustomerResult lockCustomer(UUID id, Attempt attempt) {
        try {
            return customers.lock(id);
        } catch (RuntimeException failure) {
            attempt.lockCallFailure = failure;
            throw failure;
        }
    }

    private com.example.agenticorderservice.product.application.ProductResult lockProduct(UUID id, Attempt attempt) {
        try {
            return products.lock(id);
        } catch (RuntimeException failure) {
            attempt.lockCallFailure = failure;
            throw failure;
        }
    }

    private void retryAfterRollback(RuntimeException failure, Attempt attempt, int number, long started) {
        if (attempt.completion != TransactionSynchronization.STATUS_ROLLED_BACK) {
            event(org.slf4j.event.Level.ERROR, "event=ORDER_OUTCOME_UNKNOWN attempt={} outcome=UNKNOWN", number + 1);
            throw new OrderFailure(TECHNICAL, failure, true);
        }
        guard.check(); // Also confirms unbound resources before any backoff/new attempt.
        if (!OrderRetryPolicy.transientFailure(failure, attempt.lockCallFailure == failure)) {
            String reason = rejectionReason(failure);
            if (reason != null) {
                event(org.slf4j.event.Level.DEBUG, "event=ORDER_REJECTED attempt={} reason={}", number + 1, reason);
            }
            throw failure;
        }
        event(org.slf4j.event.Level.DEBUG, "event=ORDER_ATTEMPT_ROLLED_BACK attempt={}", number + 1);
        long delay = TimeUnit.MILLISECONDS.toNanos(number == 0 ? 25 : 50);
        if (number == 2 || clock.getAsLong() - started + delay >= TimeUnit.SECONDS.toNanos(10)) {
            event(
                    org.slf4j.event.Level.ERROR,
                    "event=ORDER_RETRY_EXHAUSTED attempts={} reason={}",
                    number + 1,
                    number == 2 ? "ATTEMPT_LIMIT" : "SCHEDULING_BUDGET");
            throw new OrderFailure(UNAVAILABLE, failure, true);
        }
        event(
                org.slf4j.event.Level.WARN,
                "event=ORDER_RETRY_SCHEDULED attempt={} nextAttempt={} delayMs={}",
                number + 1,
                number + 2,
                TimeUnit.NANOSECONDS.toMillis(delay));
        pauseBeforeRetry(delay, number);
        if (clock.getAsLong() - started >= TimeUnit.SECONDS.toNanos(10)) {
            event(
                    org.slf4j.event.Level.ERROR,
                    "event=ORDER_RETRY_EXHAUSTED attempts={} reason=SCHEDULING_BUDGET",
                    number + 1);
            throw new OrderFailure(UNAVAILABLE, failure, true);
        }
    }

    private static String rejectionReason(RuntimeException failure) {
        if (failure instanceof com.example.agenticorderservice.customer.application.CustomerNotFoundException)
            return "CUSTOMER_NOT_FOUND";
        if (failure instanceof com.example.agenticorderservice.product.application.ProductNotFoundException)
            return "PRODUCT_NOT_FOUND";
        if (failure instanceof OrderFailure expected
                && (expected.reason() == CUSTOMER_INACTIVE || expected.reason() == PRODUCT_INACTIVE))
            return expected.reason().name();
        return null;
    }

    private void pauseBeforeRetry(long delay, int number) {
        try {
            pause.accept(delay);
        } catch (RuntimeException interrupted) {
            // This is a confirmed rollback with aborted scheduling, never an uncertain commit.
            event(
                    org.slf4j.event.Level.ERROR,
                    "event=ORDER_RETRY_EXHAUSTED attempts={} reason={}",
                    number + 1,
                    interrupted.getCause() instanceof InterruptedException ? "BACKOFF_INTERRUPTED" : "BACKOFF_FAILED");
            if (interrupted instanceof OrderFailure terminal) throw terminal.withDiagnosticOwnership();
            throw interrupted;
        }
    }

    private static void event(org.slf4j.event.Level level, String template, Object... fields) {
        com.example.agenticorderservice.shared.logging.SafeEvent.emit(
                org.slf4j.LoggerFactory.getLogger(OrderPlacementCoordinator.class), level, template, fields);
    }
}
