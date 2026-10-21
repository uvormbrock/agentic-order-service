package com.example.agenticorderservice.order.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.customer.application.*;
import com.example.agenticorderservice.order.integration.*;
import com.example.agenticorderservice.order.persistence.OrderRepository;
import com.example.agenticorderservice.order.persistence.OrderWriteAdapter;
import com.example.agenticorderservice.order.transaction.*;
import com.example.agenticorderservice.product.application.*;
import com.example.agenticorderservice.shared.api.PostgreSqlApiTestSupport;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

@org.springframework.test.context.TestPropertySource(properties = "logging.level.org.hibernate.SQL=DEBUG")
class OrderPlacementConcurrencyIT extends PostgreSqlApiTestSupport {
    com.example.agenticorderservice.shared.logging.EventCapture orderEvents;

    @org.junit.jupiter.api.BeforeEach
    void captureOrderEvents() {
        orderEvents = new com.example.agenticorderservice.shared.logging.EventCapture(OrderPlacementCoordinator.class);
    }

    @org.junit.jupiter.api.AfterEach
    void releaseOrderEvents() {
        orderEvents.close();
    }

    @Autowired
    OrderPlacementCoordinator placement;

    @Autowired
    CustomerApplicationService customerService;

    @Autowired
    ProductApplicationService productService;

    @Autowired
    PlatformTransactionManager manager;

    @Autowired
    OrderRepository orders;

    @MockitoSpyBean
    ProductOrderAccess products;

    @MockitoSpyBean
    CustomerOrderAccess customers;

    ProductOrderAccess productSpy() {
        return org.springframework.test.util.AopTestUtils.getUltimateTargetObject(products);
    }

    CustomerOrderAccess customerSpy() {
        return org.springframework.test.util.AopTestUtils.getUltimateTargetObject(customers);
    }

    UUID customer() {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into customers(id,name,email,active,version) values (?,'Current',?,true,0)",
                id,
                id + "@example.com");
        return id;
    }

    UUID product() {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into products(id,name,price,active,version) values (?,'Original',25,true,0)", id);
        return id;
    }

    PlaceOrderCommand command(UUID c, UUID p) {
        return new PlaceOrderCommand(c, List.of(new PlaceOrderCommand.Line(p, 4)));
    }

    <T> T tx(java.util.function.Supplier<T> work) {
        return new TransactionTemplate(manager).execute(status -> work.get());
    }

    static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(12, TimeUnit.SECONDS))
                    .as("fixture synchronization deadline")
                    .isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    void blocked(String table) {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        do {
            int count = jdbc.queryForObject(
                    "select count(*) from pg_stat_activity where datname=current_database() and wait_event_type='Lock' and query like ?",
                    Integer.class,
                    "%" + table + "%");
            if (count > 0) return;
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(2));
        } while (System.nanoTime() < end);
        fail("No PostgreSQL lock waiter observed for " + table);
    }

    void noOrder(UUID c) {
        assertThat(jdbc.queryForObject("select count(*) from orders where customer_id=?", Integer.class, c))
                .isZero();
    }

    @ParameterizedTest
    @CsvSource({
        "product,update,false",
        "product,update,true",
        "product,deactivate,false",
        "product,deactivate,true",
        "customer,update,false",
        "customer,update,true",
        "customer,deactivate,false",
        "customer,deactivate,true"
    })
    void placementAndMutationHaveBothSeriallyValidWinnerOrders(String feature, String operation, boolean placementFirst)
            throws Exception {
        UUID c = customer(), p = product();
        UUID resource = feature.equals("product") ? p : c;
        var held = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        java.util.function.Supplier<Void> mutate = () -> {
            if (feature.equals("product")) {
                if (operation.equals("update")) productService.update(p, "Changed", new java.math.BigDecimal("30"));
                else productService.deactivate(p);
            } else {
                if (operation.equals("update")) customerService.update(c, "Changed", c + "@example.com");
                else customerService.deactivate(c);
            }
            return null;
        };
        if (placementFirst) {
            doAnswer(call -> {
                        var result = call.callRealMethod();
                        if (call.getArgument(0).equals(p)) {
                            held.countDown();
                            await(release);
                        }
                        return result;
                    })
                    .when(productSpy())
                    .lock(any());
        }
        try (var pool = Executors.newFixedThreadPool(2)) {
            try {
                if (placementFirst) {
                    var order = pool.submit(() -> placement.place(command(c, p)));
                    await(held);
                    var writer = pool.submit(mutate::get);
                    blocked(feature + "s");
                    release.countDown();
                    var result = order.get(15, TimeUnit.SECONDS);
                    writer.get(15, TimeUnit.SECONDS);
                    assertThat(result.items().getFirst().unitPrice()).isEqualByComparingTo("25");
                    assertThat(result.items().getFirst().productName()).isEqualTo("Original");
                    assertThat(result.total()).isEqualByComparingTo("90");
                } else {
                    var writer = pool.submit(() -> tx(() -> {
                        mutate.get();
                        held.countDown();
                        await(release);
                        return null;
                    }));
                    await(held);
                    var order = pool.submit(() -> placement.place(command(c, p)));
                    blocked(feature + "s");
                    release.countDown();
                    writer.get(15, TimeUnit.SECONDS);
                    if (operation.equals("deactivate")) {
                        Throwable failure = catchThrowable(() -> order.get(15, TimeUnit.SECONDS));
                        assertThat(failure).hasCauseInstanceOf(OrderFailure.class);
                        var expected = feature.equals("product")
                                ? OrderFailure.Reason.PRODUCT_INACTIVE
                                : OrderFailure.Reason.CUSTOMER_INACTIVE;
                        assertThat(((OrderFailure) failure.getCause()).reason()).isEqualTo(expected);
                        noOrder(c);
                    } else {
                        var result = order.get(15, TimeUnit.SECONDS);
                        if (feature.equals("product")) {
                            assertThat(result.items().getFirst().unitPrice()).isEqualByComparingTo("30");
                            assertThat(result.total()).isEqualByComparingTo("108");
                        } else assertThat(result.customer().name()).isEqualTo("Changed");
                    }
                }
            } finally {
                release.countDown();
            }
        }
        assertThat(jdbc.queryForObject("select version from " + feature + "s where id=?", Long.class, resource))
                .isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void deleteVersusFirstOrderUsesPostWaitReferenceVisibility(boolean placementFirst) throws Exception {
        UUID c = customer(), p = product();
        var held = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        if (placementFirst)
            doAnswer(call -> {
                        var result = call.callRealMethod();
                        if (call.getArgument(0).equals(c)) {
                            held.countDown();
                            await(release);
                        }
                        return result;
                    })
                    .when(customerSpy())
                    .lock(any());
        try (var pool = Executors.newFixedThreadPool(2)) {
            try {
                if (placementFirst) {
                    var order = pool.submit(() -> placement.place(command(c, p)));
                    await(held);
                    var deletion = pool.submit(() -> customerService.delete(c));
                    blocked("customers");
                    release.countDown();
                    assertThat(order.get(15, TimeUnit.SECONDS).customerId()).isEqualTo(c);
                    assertThatThrownBy(() -> deletion.get(15, TimeUnit.SECONDS))
                            .hasCauseInstanceOf(
                                    com.example.agenticorderservice.customer.application.CustomerHasOrdersException
                                            .class);
                    assertThat(jdbc.queryForObject("select version from customers where id=?", Long.class, c))
                            .isZero();
                    assertThat(jdbc.queryForObject("select active from customers where id=?", Boolean.class, c))
                            .isTrue();
                    assertThat(jdbc.queryForObject("select count(*) from orders where customer_id=?", Integer.class, c))
                            .isEqualTo(1);
                } else {
                    var deletion = pool.submit(() -> tx(() -> {
                        customerService.delete(c);
                        held.countDown();
                        await(release);
                        return null;
                    }));
                    await(held);
                    var order = pool.submit(() -> placement.place(command(c, p)));
                    blocked("customers");
                    release.countDown();
                    deletion.get(15, TimeUnit.SECONDS);
                    assertThatThrownBy(() -> order.get(15, TimeUnit.SECONDS))
                            .hasCauseInstanceOf(CustomerNotFoundException.class);
                    noOrder(c);
                    assertThat(jdbc.queryForObject("select count(*) from customers where id=?", Integer.class, c))
                            .isZero();
                }
            } finally {
                release.countDown();
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void reverseInputOrdersAcquireIndividualCanonicalLocksAndSerializeSharedProducts(boolean sameCustomer)
            throws Exception {
        UUID c = customer(), other = sameCustomer ? c : customer(), a = product(), b = product();
        var ids = new ArrayList<>(List.of(a, b));
        ids.sort(Comparator.comparing(UUID::toString));
        UUID first = ids.getFirst(), last = ids.getLast();
        var held = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var observed = new ConcurrentHashMap<String, List<UUID>>();
        doAnswer(call -> {
                    UUID id = call.getArgument(0);
                    var result = call.callRealMethod();
                    observed.computeIfAbsent(Thread.currentThread().getName(), key -> new ArrayList<>())
                            .add(id);
                    if (id.equals(first) && held.getCount() > 0) {
                        held.countDown();
                        await(release);
                    }
                    return result;
                })
                .when(productSpy())
                .lock(any());
        try (var pool = Executors.newFixedThreadPool(2)) {
            try {
                var x = pool.submit(() -> placement.place(new PlaceOrderCommand(
                        c, List.of(new PlaceOrderCommand.Line(last, 2), new PlaceOrderCommand.Line(first, 2)))));
                await(held);
                var y = pool.submit(() -> placement.place(new PlaceOrderCommand(
                        other, List.of(new PlaceOrderCommand.Line(first, 2), new PlaceOrderCommand.Line(last, 2)))));
                blocked(sameCustomer ? "customers" : "products");
                release.countDown();
                assertThat(x.get(15, TimeUnit.SECONDS).total()).isEqualByComparingTo("90");
                assertThat(y.get(15, TimeUnit.SECONDS).total()).isEqualByComparingTo("90");
                assertThat(observed.values())
                        .hasSize(2)
                        .allSatisfy(order -> assertThat(order).containsExactly(first, last));
            } finally {
                release.countDown();
            }
        }
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"customers", "products"})
    void localLockTimeoutExhaustsThreeFreshAttemptsWithoutRows(String table) throws Exception {
        UUID c = customer(), p = product();
        var held = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var contexts = new ArrayList<Object>();
        doAnswer(call -> {
                    assertThat(jdbc.queryForObject("show transaction_isolation", String.class))
                            .isEqualTo("read committed");
                    assertThat(jdbc.queryForObject("show lock_timeout", String.class))
                            .isEqualTo("2s");
                    contexts.add(EntityManagerFactoryUtils.getTransactionalEntityManager(factory));
                    return call.callRealMethod();
                })
                .when(customerSpy())
                .lock(c);
        try (var pool = Executors.newSingleThreadExecutor()) {
            try {
                var holder = pool.submit(() -> tx(() -> {
                    jdbc.queryForObject(
                            "select id from " + table + " where id=? for update",
                            UUID.class,
                            table.equals("customers") ? c : p);
                    held.countDown();
                    await(release);
                    return null;
                }));
                await(held);
                var input = command(c, p);
                assertThatExceptionOfType(OrderFailure.class)
                        .isThrownBy(() -> placement.place(input))
                        .satisfies(f -> assertThat(f.reason()).isEqualTo(OrderFailure.Reason.UNAVAILABLE));
                assertThat(orderEvents.messages().stream()
                                .filter(event -> event.contains("ORDER_ATTEMPT_ROLLED_BACK"))
                                .count())
                        .isEqualTo(3);
                assertThat(orderEvents.messages().stream()
                                .filter(event -> event.contains("ORDER_RETRY_SCHEDULED"))
                                .count())
                        .isEqualTo(2);
                assertThat(orderEvents.messages().stream()
                                .filter(event -> event.contains("ORDER_RETRY_EXHAUSTED"))
                                .count())
                        .isEqualTo(1);
                assertThat(orderEvents.messages()).noneMatch(event -> event.contains("ORDER_COMMITTED"));
                assertThat(contexts).hasSize(3);
                var identities = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
                identities.addAll(contexts);
                assertThat(identities).hasSize(3);
                noOrder(c);
                release.countDown();
                holder.get(15, TimeUnit.SECONDS);
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void retryReadsCommittedCustomerAndProductFactsAfterCompleteRollback() {
        UUID c = customer(), p = product();
        var contexts = new ArrayList<Object>();
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var pauses = new java.util.concurrent.atomic.AtomicInteger();
        doAnswer(call -> {
                    contexts.add(EntityManagerFactoryUtils.getTransactionalEntityManager(factory));
                    var result = call.callRealMethod();
                    if (calls.incrementAndGet() == 1) {
                        throw new RuntimeException(new SQLException("Test-owned eligible failure", "40P01"));
                    }
                    return result;
                })
                .when(productSpy())
                .lock(p);
        var coordinator = new OrderPlacementCoordinator(
                new SpringOrderPlacementAttempts(new TransactionTemplate(manager), jdbc, new AmbientTransactionGuard()),
                new OrderPlacementCustomerAdapter(customers),
                new OrderPlacementProductAdapter(products),
                new OrderWriteAdapter(orders),
                System::nanoTime,
                delay -> {
                    assertThat(delay).isEqualTo(TimeUnit.MILLISECONDS.toNanos(25));
                    new AmbientTransactionGuard().check();
                    noOrder(c);
                    assertThat(jdbc.queryForObject(
                                    "select count(*) from order_items where product_id=?", Integer.class, p))
                            .isZero();
                    tx(() -> {
                        customerService.update(c, "Fresh customer", c + "@example.com");
                        productService.update(p, "Fresh product", new java.math.BigDecimal("30.00"));
                        return null;
                    });
                    pauses.incrementAndGet();
                });
        var result = coordinator.place(command(c, p));
        assertThat(pauses.get()).isEqualTo(1);
        assertThat(calls.get()).isEqualTo(2);
        assertThat(contexts).hasSize(2).doesNotContainNull();
        assertThat(contexts.get(0)).isNotSameAs(contexts.get(1));
        assertThat(result.customer().name()).isEqualTo("Fresh customer");
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.productName()).isEqualTo("Fresh product");
            assertThat(item.unitPrice()).isEqualByComparingTo("30.00");
            assertThat(item.quantity()).isEqualTo(4);
        });
        assertThat(result.subtotal()).isEqualByComparingTo("120.00");
        assertThat(result.discount()).isEqualByComparingTo("12.00");
        assertThat(result.total()).isEqualByComparingTo("108.00");
        assertThat(jdbc.queryForObject("select count(*) from orders where customer_id=?", Integer.class, c))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from order_items where order_id=?", Integer.class, result.id()))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "select product_name from order_items where order_id=?", String.class, result.id()))
                .isEqualTo("Fresh product");
    }

    @Test
    void actualCommitWithLostConfirmationNeverReplays() {
        UUID c = customer(), p = product();
        var commits = new java.util.concurrent.atomic.AtomicInteger();
        PlatformTransactionManager lost = new PlatformTransactionManager() {
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return manager.getTransaction(definition);
            }

            public void rollback(TransactionStatus status) {
                manager.rollback(status);
            }

            public void commit(TransactionStatus status) {
                manager.commit(status);
                commits.incrementAndGet();
                throw new TransactionSystemException("Injected lost commit confirmation");
            }
        };
        var coordinator = new OrderPlacementCoordinator(
                new SpringOrderPlacementAttempts(new TransactionTemplate(lost), jdbc, new AmbientTransactionGuard()),
                new OrderPlacementCustomerAdapter(customers),
                new OrderPlacementProductAdapter(products),
                new OrderWriteAdapter(orders));
        var input = command(c, p);
        assertThatExceptionOfType(OrderFailure.class)
                .isThrownBy(() -> coordinator.place(input))
                .satisfies(f -> assertThat(f.reason()).isEqualTo(OrderFailure.Reason.TECHNICAL));
        assertThat(orderEvents.messages()).singleElement().asString().contains("ORDER_OUTCOME_UNKNOWN");
        assertThat(commits.get()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from orders where customer_id=?", Integer.class, c))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "select count(*) from order_items i join orders o on o.id=i.order_id where o.customer_id=?",
                        Integer.class,
                        c))
                .isEqualTo(1);
    }

    @Test
    void ambientTransactionIsRejectedBeforeProviderAccess() {
        UUID c = customer(), p = product();
        tx(() -> {
            assertThatIllegalStateException().isThrownBy(() -> placement.place(command(c, p)));
            verifyNoInteractions(customerSpy(), productSpy());
            return null;
        });
        noOrder(c);
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void atomicMultiProductWriterCannotProduceIncompatibleSnapshots(boolean writerFirst) throws Exception {
        UUID c = customer(), a = product(), b = product();
        var ids = new ArrayList<>(List.of(a, b));
        ids.sort(Comparator.comparing(UUID::toString));
        var held = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        java.util.function.Supplier<Void> write = () -> tx(() -> {
            productService.update(ids.getFirst(), "New first", new java.math.BigDecimal("30"));
            if (writerFirst) {
                held.countDown();
                await(release);
            }
            productService.update(ids.getLast(), "New last", new java.math.BigDecimal("40"));
            return null;
        });
        java.util.function.Supplier<OrderResult> place = () -> placement.place(new PlaceOrderCommand(
                c,
                List.of(new PlaceOrderCommand.Line(ids.getLast(), 2), new PlaceOrderCommand.Line(ids.getFirst(), 2))));
        if (!writerFirst)
            doAnswer(call -> {
                        var result = call.callRealMethod();
                        if (call.getArgument(0).equals(ids.getFirst())) {
                            held.countDown();
                            await(release);
                        }
                        return result;
                    })
                    .when(productSpy())
                    .lock(any());
        try (var pool = Executors.newFixedThreadPool(2)) {
            try {
                Future<Void> writer;
                Future<OrderResult> order;
                if (writerFirst) {
                    writer = pool.submit(write::get);
                    await(held);
                    order = pool.submit(place::get);
                } else {
                    order = pool.submit(place::get);
                    await(held);
                    writer = pool.submit(write::get);
                }
                blocked("products");
                release.countDown();
                writer.get(15, TimeUnit.SECONDS);
                var result = order.get(15, TimeUnit.SECONDS);
                if (writerFirst) {
                    assertThat(result.items())
                            .extracting(OrderResult.Item::productName)
                            .containsExactly("New first", "New last");
                    assertThat(result.subtotal()).isEqualByComparingTo("140");
                    assertThat(result.total()).isEqualByComparingTo("126");
                } else {
                    assertThat(result.items())
                            .extracting(OrderResult.Item::productName)
                            .containsExactly("Original", "Original");
                    assertThat(result.subtotal()).isEqualByComparingTo("100");
                    assertThat(result.total()).isEqualByComparingTo("90");
                }
            } finally {
                release.countDown();
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"40P01", "40001", "23514", "57014", "55P03"})
    void databaseFlushFaultsRequireCompleteRollbackAndApprovedClassification(String state) {
        UUID c = customer(), p = product(), other = product();
        var faultProducts = new ArrayList<>(List.of(p, other));
        faultProducts.sort(Comparator.comparing(UUID::toString));
        var input = new PlaceOrderCommand(
                c, List.of(new PlaceOrderCommand.Line(p, 2), new PlaceOrderCommand.Line(other, 2)));
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String sequence = "fault_sequence_" + suffix,
                function = "fault_function_" + suffix,
                trigger = "fault_trigger_" + suffix;
        var contexts = new ArrayList<Object>();
        doAnswer(call -> {
                    noOrder(c);
                    contexts.add(EntityManagerFactoryUtils.getTransactionalEntityManager(factory));
                    return call.callRealMethod();
                })
                .when(customerSpy())
                .lock(c);
        jdbc.execute("create sequence " + sequence);
        // Fail the second owned-item INSERT: the root and first item already exist inside the failed transaction.
        // Sequence increments survive rollback, allowing a third fresh attempt to succeed.
        jdbc.execute("create function " + function
                + "() returns trigger language plpgsql as $$ begin if NEW.product_id='" + faultProducts.getLast()
                + "' and exists(select 1 from orders where id=NEW.order_id and customer_id='" + c + "') and nextval('"
                + sequence + "') <= 2 then raise exception 'Test-owned flush fault' using errcode='" + state
                + "'; end if; return NEW; end $$");
        jdbc.execute("create trigger " + trigger + " before insert on order_items for each row execute function "
                + function + "()");
        try {
            if (Set.of("40P01", "40001").contains(state)) {
                var result = placement.place(input);
                assertThat(result.total()).isEqualByComparingTo("90");
                assertThat(contexts).hasSize(3);
                var identities = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
                identities.addAll(contexts);
                assertThat(identities).hasSize(3);
                assertThat(jdbc.queryForObject("select count(*) from orders where customer_id=?", Integer.class, c))
                        .isEqualTo(1);
                assertThat(jdbc.queryForObject(
                                "select count(*) from order_items i join orders o on o.id=i.order_id where o.customer_id=?",
                                Integer.class,
                                c))
                        .isEqualTo(2);
            } else {
                assertThatThrownBy(() -> placement.place(input)).isInstanceOf(RuntimeException.class);
                assertThat(contexts).hasSize(1);
                noOrder(c);
            }
            assertThat(jdbc.queryForObject(
                            "select count(*) from order_items i left join orders o on o.id=i.order_id where o.id is null",
                            Integer.class))
                    .isZero();
        } finally {
            jdbc.execute("drop trigger " + trigger + " on order_items");
            jdbc.execute("drop function " + function + "()");
            jdbc.execute("drop sequence " + sequence);
        }
    }

    @Test
    void controlledOppositeFixtureLockOrderCausesRealDeadlockWithoutAssumingVictim() throws Exception {
        UUID a = product(), b = product();
        var both = new CyclicBarrier(2);
        java.util.function.BiFunction<UUID, UUID, String> lock = (first, second) -> {
            try {
                tx(() -> {
                    jdbc.execute("SET LOCAL lock_timeout = '5s'");
                    jdbc.queryForObject("select id from products where id=? for update", UUID.class, first);
                    try {
                        both.await(10, TimeUnit.SECONDS);
                    } catch (Exception failure) {
                        throw new IllegalStateException(failure);
                    }
                    jdbc.queryForObject("select id from products where id=? for update", UUID.class, second);
                    return null;
                });
                return "COMMITTED";
            } catch (RuntimeException failure) {
                for (Throwable cause = failure; cause != null; cause = cause.getCause())
                    if (cause instanceof SQLException sql) return sql.getSQLState();
                throw failure;
            }
        };
        try (var pool = Executors.newFixedThreadPool(2)) {
            var one = pool.submit(() -> lock.apply(a, b));
            var two = pool.submit(() -> lock.apply(b, a));
            assertThat(List.of(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("COMMITTED", "40P01");
        }
    }

    @Test
    void nowaitFailureIsNotTheConfiguredLockTimeout() throws Exception {
        UUID p = product();
        var held = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var pool = Executors.newSingleThreadExecutor()) {
            try {
                var holder = pool.submit(() -> tx(() -> {
                    jdbc.queryForObject("select id from products where id=? for update", UUID.class, p);
                    held.countDown();
                    await(release);
                    return null;
                }));
                await(held);
                Throwable failure = catchThrowable(() -> tx(() ->
                        jdbc.queryForObject("select id from products where id=? for update nowait", UUID.class, p)));
                assertThat(failure).isNotNull();
                assertThat(failure).isInstanceOf(RuntimeException.class);
                assertThat(new SpringOrderPlacementAttempts(
                                        new TransactionTemplate(manager), jdbc, new AmbientTransactionGuard())
                                .transientFailure((RuntimeException) failure, true))
                        .isFalse();
                Throwable root = failure;
                while (root.getCause() != null) root = root.getCause();
                assertThat(root).isInstanceOf(org.postgresql.util.PSQLException.class);
                var sql = (org.postgresql.util.PSQLException) root;
                assertThat(sql.getSQLState()).isEqualTo("55P03");
                assertThat(sql.getServerErrorMessage().getRoutine()).isNotEqualTo("ProcessInterrupts");
                release.countDown();
                holder.get(15, TimeUnit.SECONDS);
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void serverRaised55P03InsideProviderCallNeverRetries() {
        UUID c = customer(), p = product();
        String function = "not_timeout_" + UUID.randomUUID().toString().replace("-", "");
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        jdbc.execute(
                "create function " + function
                        + "() returns uuid language plpgsql as $$ begin raise exception 'Test-owned non-timeout lock error' using errcode='55P03'; end $$");
        try {
            doAnswer(call -> {
                        calls.incrementAndGet();
                        return jdbc.queryForObject("select " + function + "()", UUID.class);
                    })
                    .when(productSpy())
                    .lock(p);
            Throwable failure = catchThrowable(() -> placement.place(command(c, p)));
            assertThat(failure).isNotNull();
            assertThat(calls.get()).isEqualTo(1);
            noOrder(c);
            Throwable root = failure;
            while (root.getCause() != null) root = root.getCause();
            assertThat(root).isInstanceOf(org.postgresql.util.PSQLException.class);
            var sql = (org.postgresql.util.PSQLException) root;
            assertThat(sql.getSQLState()).isEqualTo("55P03");
            assertThat(sql.getServerErrorMessage().getRoutine()).isEqualTo("exec_stmt_raise");
        } finally {
            jdbc.execute("drop function " + function + "()");
        }
    }

    @Test
    void realOrderCommitEventFollowsDatabaseCompletionAndCustomerDeleteWaitsForOuterCommit() {
        UUID c = customer(), p = product();
        doAnswer(call -> {
                    assertThat(orderEvents.messages()).noneMatch(event -> event.contains("ORDER_COMMITTED"));
                    return call.callRealMethod();
                })
                .when(productSpy())
                .lock(p);
        var result = placement.place(command(c, p));
        assertThat(orderEvents.messages()).singleElement().asString().contains("ORDER_COMMITTED");
        assertThat(jdbc.queryForObject("select count(*) from orders where id=?", Integer.class, result.id()))
                .isEqualTo(1);
        try (var events =
                new com.example.agenticorderservice.shared.logging.EventCapture(CustomerApplicationService.class)) {
            assertThatThrownBy(() -> customerService.delete(c))
                    .isInstanceOf(
                            com.example.agenticorderservice.customer.application.CustomerHasOrdersException.class);
            assertThat(events.messages()).isEmpty();
            assertThat(jdbc.queryForObject("select active from customers where id=?", Boolean.class, c))
                    .isTrue();
            UUID unreferenced = customer();
            tx(() -> {
                customerService.delete(unreferenced);
                assertThat(events.messages()).isEmpty();
                return null;
            });
            assertThat(events.messages()).containsExactly("event=CUSTOMER_DELETED");
            assertThat(jdbc.queryForObject("select count(*) from customers where id=?", Integer.class, unreferenced))
                    .isZero();
            UUID rollback = customer();
            new TransactionTemplate(manager).execute(status -> {
                customerService.delete(rollback);
                status.setRollbackOnly();
                return null;
            });
            assertThat(events.messages()).hasSize(1);
            assertThat(jdbc.queryForObject("select count(*) from customers where id=?", Integer.class, rollback))
                    .isEqualTo(1);
        }
    }
}
