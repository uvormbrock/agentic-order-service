package com.example.agenticorderservice.order.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.customer.application.CustomerApplicationService;
import com.example.agenticorderservice.customer.application.CustomerOrderAccess;
import com.example.agenticorderservice.order.integration.*;
import com.example.agenticorderservice.order.persistence.OrderRepository;
import com.example.agenticorderservice.order.persistence.OrderWriteAdapter;
import com.example.agenticorderservice.order.transaction.*;
import com.example.agenticorderservice.product.application.ProductOrderAccess;
import com.example.agenticorderservice.shared.api.PostgreSqlApiTestSupport;
import com.example.agenticorderservice.shared.logging.EventCapture;
import jakarta.persistence.EntityManagerFactory;
import java.net.URI;
import java.net.http.*;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import org.springframework.web.bind.annotation.*;

@Import(AsyncTransactionLoggingIT.Fixture.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AsyncTransactionLoggingIT extends PostgreSqlApiTestSupport {
    @Autowired
    Probes probes;

    @Autowired
    CustomerApplicationService customerService;

    @Autowired
    OrderPlacementCoordinator placement;

    @MockitoSpyBean
    ProductOrderAccess productsAccess;

    static class Probe {
        final CountDownLatch businessDone = new CountDownLatch(1), release = new CountDownLatch(1);
        final List<Integer> completions = new CopyOnWriteArrayList<>();
        volatile String workerId;
        volatile boolean ambient, resources, resourcesAfter;
        volatile long beforeCommit = -1, completionEvents = -1, insideDelete = -1;
        EventCapture capture;

        long successes() {
            return capture.events.stream()
                    .filter(e -> Objects.equals(workerId, e.getMDCPropertyMap().get("requestId"))
                            && e.getFormattedMessage().contains("event=ORDER_COMMITTED "))
                    .count();
        }

        void observeTransaction() {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    beforeCommit = successes();
                }

                @Override
                public void afterCompletion(int status) {
                    completionEvents = successes();
                    completions.add(status);
                }
            });
        }
    }

    static class Probes {
        final ConcurrentMap<String, Probe> values = new ConcurrentHashMap<>();
    }

    static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

    UUID customer() {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into customers(id,name,email,active,version) values (?,'Fixture',?,true,0)",
                id,
                id + "@example.com");
        return id;
    }

    UUID product() {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into products(id,name,price,active,version) values (?,'Fixture',25,true,0)", id);
        return id;
    }

    static PlaceOrderCommand command(UUID customer, UUID product) {
        return new PlaceOrderCommand(customer, List.of(new PlaceOrderCommand.Line(product, 4)));
    }

    CompletableFuture<HttpResponse<String>> invoke(String kind, String mode, UUID customer, UUID product, String key) {
        return client.sendAsync(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/test-async-logging/" + kind + "/"
                                + mode + "/" + customer + "/" + product + "?key=" + key))
                        .timeout(Duration.ofSeconds(20))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"COMMIT", "RETRY", "REJECT", "UNKNOWN"})
    void asyncOrderKeepsCorrelationTransactionIsolationAndHttpCompletionSeparate(String mode) throws Exception {
        UUID c = customer(), p = product();
        String key = UUID.randomUUID().toString();
        var probe = new Probe();
        if (mode.equals("REJECT")) jdbc.update("update products set active=false where id=?", p);
        var calls = new AtomicInteger();
        ProductOrderAccess target = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(productsAccess);
        doAnswer(call -> {
                    probe.observeTransaction();
                    if (calls.incrementAndGet() == 1 && mode.equals("RETRY"))
                        throw new RuntimeException(new SQLException("fixture retry", "40001"));
                    return call.callRealMethod();
                })
                .when(target)
                .lock(p);
        try (var capture = new EventCapture("com.example.agenticorderservice")) {
            probe.capture = capture;
            probes.values.put(key, probe);
            var response = invoke("order", mode, c, p, key);
            await(probe.businessDone);
            assertThat(probe.workerId).isNotNull();
            assertThat(UUID.fromString(probe.workerId)).hasToString(probe.workerId);
            assertThat(probe.ambient).isFalse();
            assertThat(probe.resources).isFalse();
            assertThat(probe.resourcesAfter).isFalse();
            assertThat(capture.events.stream()
                            .filter(e -> Objects.equals(
                                            probe.workerId,
                                            e.getMDCPropertyMap().get("requestId"))
                                    && e.getFormattedMessage().contains("HTTP_REQUEST_COMPLETED"))
                            .count())
                    .isZero();
            if (mode.equals("COMMIT") || mode.equals("RETRY")) {
                assertThat(probe.beforeCommit).isZero();
                assertThat(probe.completionEvents).isZero();
                assertThat(probe.successes()).isEqualTo(1);
                assertThat(jdbc.queryForObject("select count(*) from orders where customer_id=?", Integer.class, c))
                        .isEqualTo(1);
            } else assertThat(probe.successes()).isZero();
            probe.release.countDown();
            var actual = response.get(20, TimeUnit.SECONDS);
            assertThat(actual.statusCode()).isEqualTo(mode.equals("UNKNOWN") ? 500 : mode.equals("REJECT") ? 409 : 200);
            assertThat(actual.headers().firstValue("X-Request-ID")).contains(probe.workerId);
            capture.await(probe.workerId, "HTTP_REQUEST_COMPLETED");
            if (mode.equals("UNKNOWN")) {
                assertThat(calls.get()).isEqualTo(1);
                assertThat(probe.completions).containsExactly(TransactionSynchronization.STATUS_UNKNOWN);
                assertThat(jdbc.queryForObject("select count(*) from orders where customer_id=?", Integer.class, c))
                        .isEqualTo(1);
                assertThat(capture.messages())
                        .anyMatch(m -> m.contains("ORDER_OUTCOME_UNKNOWN") && m.contains("outcome=UNKNOWN"));
                assertThat(capture.messages()).noneMatch(m -> m.contains("HTTP_TECHNICAL_FAILURE"));
            } else if (mode.equals("RETRY")) {
                assertThat(probe.completions)
                        .containsExactly(
                                TransactionSynchronization.STATUS_ROLLED_BACK,
                                TransactionSynchronization.STATUS_COMMITTED);
                assertThat(calls.get()).isEqualTo(2);
                assertThat(capture.messages())
                        .contains("event=ORDER_RETRY_SCHEDULED attempt=1 nextAttempt=2 delayMs=25");
            } else if (mode.equals("REJECT")) {
                assertThat(probe.completions).containsExactly(TransactionSynchronization.STATUS_ROLLED_BACK);
                assertThat(jdbc.queryForObject("select count(*) from orders where customer_id=?", Integer.class, c))
                        .isZero();
            }
            assertThat(capture.messages())
                    .allSatisfy(m -> assertThat(m).doesNotContain(c.toString(), p.toString(), "fixture retry"));
        } finally {
            probe.release.countDown();
            probes.values.remove(key);
        }
    }

    @ParameterizedTest
    @CsvSource({"COMMIT", "ROLLBACK", "UNKNOWN"})
    void asyncCustomerDeleteReportsOnlyConfirmedCommit(String completion) throws Exception {
        UUID c = customer(), p = product();
        String key = UUID.randomUUID().toString();
        var probe = new Probe();
        try (var capture = new EventCapture("com.example.agenticorderservice")) {
            probe.capture = capture;
            probes.values.put(key, probe);
            var response = invoke("delete", completion, c, p, key);
            await(probe.businessDone);
            assertThat(probe.ambient).isFalse();
            assertThat(probe.resources).isFalse();
            assertThat(probe.resourcesAfter).isFalse();
            assertThat(probe.insideDelete).isZero();
            probe.release.countDown();
            var actual = response.get(20, TimeUnit.SECONDS);
            assertThat(actual.statusCode()).isEqualTo(completion.equals("UNKNOWN") ? 500 : 200);
            assertThat(actual.headers().firstValue("X-Request-ID")).contains(probe.workerId);
            capture.await(probe.workerId, "HTTP_REQUEST_COMPLETED");
            var deletion = capture.events.stream()
                    .filter(e -> e.getFormattedMessage().startsWith("event=CUSTOMER_"))
                    .toList();
            if (completion.equals("COMMIT")) {
                assertThat(deletion).hasSize(1);
                assertThat(deletion.getFirst().getMDCPropertyMap()).containsEntry("requestId", probe.workerId);
                assertThat(deletion.getFirst().getFormattedMessage()).isEqualTo("event=CUSTOMER_DELETED");
            } else assertThat(deletion).isEmpty();
            // UNKNOWN deliberately commits before losing confirmation; it is never claimed to roll back.
            int rows = jdbc.queryForObject("select count(*) from customers where id=?", Integer.class, c);
            assertThat(rows).isEqualTo(completion.equals("ROLLBACK") ? 1 : 0);
        } finally {
            probe.release.countDown();
            probes.values.remove(key);
        }
    }

    @RestController
    static class AsyncOperations {
        final Probes probes;
        final OrderPlacementCoordinator placement;
        final CustomerApplicationService customers;
        final PlatformTransactionManager manager;
        final EntityManagerFactory factory;
        final JdbcTemplate jdbc;
        final CustomerOrderAccess customerAccess;
        final ProductOrderAccess productAccess;
        final OrderRepository orders;

        AsyncOperations(
                Probes probes,
                OrderPlacementCoordinator placement,
                CustomerApplicationService customers,
                PlatformTransactionManager manager,
                EntityManagerFactory factory,
                JdbcTemplate jdbc,
                CustomerOrderAccess customerAccess,
                ProductOrderAccess productAccess,
                OrderRepository orders) {
            this.probes = probes;
            this.placement = placement;
            this.customers = customers;
            this.manager = manager;
            this.factory = factory;
            this.jdbc = jdbc;
            this.customerAccess = customerAccess;
            this.productAccess = productAccess;
            this.orders = orders;
        }

        PlatformTransactionManager lostConfirmation() {
            return new JpaTransactionManager(factory) {
                @Override
                protected void doCommit(DefaultTransactionStatus status) {
                    super.doCommit(status);
                    throw new TransactionSystemException("fixture lost confirmation");
                }
            };
        }

        @GetMapping("/test-async-logging/{kind}/{mode}/{customer}/{product}")
        Callable<String> execute(
                @PathVariable String kind,
                @PathVariable String mode,
                @PathVariable UUID customer,
                @PathVariable UUID product,
                @RequestParam String key) {
            Probe probe = probes.values.get(key);
            return () -> {
                probe.workerId = MDC.get("requestId");
                probe.ambient = TransactionSynchronizationManager.isActualTransactionActive();
                probe.resources =
                        !TransactionSynchronizationManager.getResourceMap().isEmpty();
                try {
                    if (kind.equals("order")) {
                        var selected = mode.equals("UNKNOWN")
                                ? new OrderPlacementCoordinator(
                                        new SpringOrderPlacementAttempts(
                                                new TransactionTemplate(lostConfirmation()),
                                                jdbc,
                                                new AmbientTransactionGuard()),
                                        new OrderPlacementCustomerAdapter(customerAccess),
                                        new OrderPlacementProductAdapter(productAccess),
                                        new OrderWriteAdapter(orders))
                                : placement;
                        selected.place(command(customer, product));
                    } else {
                        new TransactionTemplate(mode.equals("UNKNOWN") ? lostConfirmation() : manager)
                                .execute(status -> {
                                    customers.delete(customer);
                                    probe.insideDelete = probe.capture.events.stream()
                                            .filter(e -> e.getFormattedMessage().startsWith("event=CUSTOMER_"))
                                            .count();
                                    if (mode.equals("ROLLBACK")) status.setRollbackOnly();
                                    return null;
                                });
                    }
                    return "done";
                } finally {
                    probe.resourcesAfter =
                            !TransactionSynchronizationManager.getResourceMap().isEmpty();
                    probe.businessDone.countDown();
                    await(probe.release);
                }
            };
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Fixture {
        @Bean
        Probes asyncTransactionProbes() {
            return new Probes();
        }

        @Bean
        AsyncOperations asyncOperations(
                Probes probes,
                OrderPlacementCoordinator placement,
                CustomerApplicationService customers,
                PlatformTransactionManager manager,
                EntityManagerFactory factory,
                JdbcTemplate jdbc,
                CustomerOrderAccess customerAccess,
                ProductOrderAccess productAccess,
                OrderRepository orders) {
            return new AsyncOperations(
                    probes, placement, customers, manager, factory, jdbc, customerAccess, productAccess, orders);
        }
    }
}
