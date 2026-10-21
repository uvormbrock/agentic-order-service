package com.example.agenticorderservice.persistence;

import static com.example.agenticorderservice.PostgreSqlTestConfiguration.inTransaction;
import static org.assertj.core.api.Assertions.*;

import com.example.agenticorderservice.customer.domain.*;
import com.example.agenticorderservice.product.domain.Product;
import com.example.agenticorderservice.shared.api.*;
import com.example.agenticorderservice.shared.domain.Money;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.*;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@org.springframework.boot.test.context.SpringBootTest(
        webEnvironment = org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.jpa.properties.hibernate.generate_statistics=true",
            "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.example.agenticorderservice.persistence.OptimisticLockingIT$VersionSqlInspector"
        })
@Import(OptimisticLockingIT.CompletionGateConfiguration.class)
class OptimisticLockingIT extends PostgreSqlApiTestSupport {
    @org.springframework.beans.factory.annotation.Autowired
    CompletionGate gate;

    private final Map<UUID, String> originalEmails = new HashMap<>();

    private record Editable(String name, String value) {}

    @ParameterizedTest
    @CsvSource({
        "product,update,update,false",
        "customer,update,update,false",
        "product,update,patch,false",
        "customer,update,patch,false",
        "product,patch,update,false",
        "customer,patch,update,false",
        "product,patch,patch,false",
        "customer,patch,patch,false",
        "product,update,update,true",
        "customer,update,update,true",
        "product,patch,update,true",
        "customer,patch,update,true"
    })
    void overlappingAndDetachedStaleMutationsProtectCommittedWinner(
            String feature, String first, String second, boolean detached) throws Exception {
        VersionSqlInspector.statements.clear();
        UUID id = create(feature);
        var loaded = new CountDownLatch(2);
        var committed = new CountDownLatch(1);
        Editable winnerValues = editable(feature, id, "Winner");
        Editable losingValues = editable(feature, id, "Loser");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var winner = executor.submit(() -> {
                try {
                    inTransaction(factory, em -> {
                        var entity = find(em, feature, id);
                        loaded.countDown();
                        await(loaded);
                        mutate(entity, first, winnerValues);
                        return null;
                    });
                } finally {
                    committed.countDown();
                }
            });
            var loser = executor.submit(() -> {
                try (var em = factory.createEntityManager()) {
                    var tx = em.getTransaction();
                    tx.begin();
                    var entity = find(em, feature, id);
                    if (detached) {
                        em.detach(entity);
                        tx.commit();
                    }
                    loaded.countDown();
                    await(loaded);
                    await(committed);
                    Throwable failure = catchThrowable(() -> {
                        if (detached) {
                            inTransaction(factory, newEm -> {
                                mutate(entity, second, losingValues);
                                newEm.merge(entity);
                                return null;
                            });
                        } else {
                            mutate(entity, second, losingValues);
                            tx.commit();
                        }
                    });
                    if (tx.isActive()) {
                        tx.rollback();
                    }
                    assertThat(failure).isNotNull();
                    assertThat(ApiExceptionHandler.classify(failure)).isEqualTo(ApiError.Code.CONCURRENT_MODIFICATION);
                }
            });
            winner.get(20, TimeUnit.SECONDS);
            loser.get(20, TimeUnit.SECONDS);
        }
        var row = jdbc.queryForMap("select name,active,version from " + feature + "s where id=?", id);
        assertThat(row).containsEntry("name", first.equals("update") ? "Winner" : "Original");
        assertThat(row).containsEntry("active", !first.equals("patch")).containsEntry("version", 1L);
        assertCommitted(
                feature,
                id,
                first.equals("update") ? winnerValues : editable(feature, id, "Original"),
                !first.equals("patch"),
                1L,
                losingValues);
        var observedSql = List.copyOf(VersionSqlInspector.statements);
        assertThat(observedSql)
                .anyMatch(sql -> sql.startsWith("update " + feature + "s ")
                        && sql.substring(sql.indexOf(" where ")).contains("version=?"));
    }

    @ParameterizedTest
    @CsvSource({"product", "customer"})
    void sequentialInactivePatchIsNoUpdateAndNoVersionIncrement(String feature) throws Exception {
        UUID id = create(feature);
        var first = request("PATCH", "/" + feature + "s/" + id, "{\"active\":false}");
        var stats = factory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.clear();
        var repeat = request("PATCH", "/" + feature + "s/" + id, "{\"active\":false}");
        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(repeat.statusCode()).isEqualTo(200);
        assertThat(repeat.body()).isEqualTo(first.body());
        assertThat(stats.getEntityUpdateCount()).isZero();
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select version from " + feature + "s where id=?", Long.class, id))
                .isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({"product", "customer"})
    void actualHttpSuccessOrConflictIsDecidedByOuterTransactionCompletion(String feature) throws Exception {
        UUID id = create(feature);
        gate.arm(id);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(
                    () -> mutationRequest(feature, id, "update", editable(feature, id, "First"), "first"));
            var second = executor.submit(
                    () -> mutationRequest(feature, id, "update", editable(feature, id, "Second"), "second"));
            var a = first.get(20, TimeUnit.SECONDS);
            var b = second.get(20, TimeUnit.SECONDS);
            assertThat(List.of(a.statusCode(), b.statusCode())).containsExactlyInAnyOrder(200, 409);
            var success = a.statusCode() == 200 ? a : b;
            var failed = a.statusCode() == 409 ? a : b;
            assertError(failed, ApiError.Code.CONCURRENT_MODIFICATION);
            assertThat(request("GET", "/" + feature + "s/" + id, null).body()).isEqualTo(success.body());
            assertThat(jdbc.queryForObject("select version from " + feature + "s where id=?", Long.class, id))
                    .isEqualTo(1);
            Editable expected = editable(feature, id, a.statusCode() == 200 ? "First" : "Second");
            Editable rejected = editable(feature, id, a.statusCode() == 200 ? "Second" : "First");
            assertResource(feature, id, json(success), expected, true);
            assertCommitted(feature, id, expected, true, 1L, rejected);
            gate.assertIndependentContexts();
            assertThat(gate.outcomes)
                    .containsEntry(
                            a.statusCode() == 200 ? "first" : "second",
                            org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED)
                    .containsEntry(
                            a.statusCode() == 409 ? "first" : "second",
                            org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK);
        } finally {
            gate.disarm();
        }
    }

    @ParameterizedTest
    @CsvSource({
        "product,update,patch",
        "customer,update,patch",
        "product,patch,update",
        "customer,patch,update",
        "product,patch,patch",
        "customer,patch,patch"
    })
    void controlledHttpDirtyWritesRejectTheStaleOperationAfterConfirmedWinnerCommit(
            String feature, String winningOperation, String losingOperation) throws Exception {
        UUID id = create(feature);
        Editable winnerValues = editable(feature, id, "Winner");
        Editable losingValues = editable(feature, id, "Loser");
        Editable expected = winningOperation.equals("update") ? winnerValues : editable(feature, id, "Original");
        boolean active = !winningOperation.equals("patch");
        gate.armOrdered(id);
        try (var executor = Executors.newFixedThreadPool(2)) {
            try {
                var winner =
                        executor.submit(() -> mutationRequest(feature, id, winningOperation, winnerValues, "winner"));
                var loser = executor.submit(() -> mutationRequest(feature, id, losingOperation, losingValues, "loser"));
                gate.awaitBoth(); // Both service bodies returned inside distinct, still-uncommitted transactions.
                gate.assertIndependentContexts();
                assertThat(gate.methods)
                        .containsEntry("winner", winningOperation.equals("update") ? "update" : "deactivate")
                        .containsEntry("loser", losingOperation.equals("update") ? "update" : "deactivate");
                assertCommitted(feature, id, editable(feature, id, "Original"), true, 0L, losingValues);
                gate.releaseWinner();
                var success = winner.get(20, TimeUnit.SECONDS);
                assertThat(success.statusCode()).isEqualTo(200);
                assertThat(gate.outcomes)
                        .containsEntry(
                                "winner",
                                org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED);
                assertResource(feature, id, json(success), expected, active);
                assertCommitted(feature, id, expected, active, 1L, losingValues);
                gate.releaseLoserAfterWinnerCompletion();
                var failure = loser.get(20, TimeUnit.SECONDS);
                assertError(failure, ApiError.Code.CONCURRENT_MODIFICATION);
                assertThat(gate.outcomes)
                        .containsEntry(
                                "loser",
                                org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK);
                assertCommitted(feature, id, expected, active, 1L, losingValues);
                assertResource(feature, id, json(request("GET", "/" + feature + "s/" + id, null)), expected, active);
            } finally {
                gate.releaseAll();
            }
        } finally {
            gate.disarm();
        }
    }

    @ParameterizedTest
    @CsvSource({"product", "customer"})
    void overlappingPatchesObservingInactiveStateCommitWithoutUpdatesOrVersionChanges(String feature) throws Exception {
        UUID id = create(feature);
        assertThat(request("PATCH", "/" + feature + "s/" + id, "{\"active\":false}")
                        .statusCode())
                .isEqualTo(200);
        Editable original = editable(feature, id, "Original");
        var stats = factory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.clear();
        gate.armOrdered(id);
        try (var executor = Executors.newFixedThreadPool(2)) {
            try {
                var first = executor.submit(() -> mutationRequest(feature, id, "patch", original, "winner"));
                var second = executor.submit(() -> mutationRequest(feature, id, "patch", original, "loser"));
                gate.awaitBoth();
                gate.assertIndependentContexts();
                assertCommitted(feature, id, original, false, 1L, editable(feature, id, "Loser"));
                gate.releaseWinner();
                var a = first.get(20, TimeUnit.SECONDS);
                assertThat(a.statusCode()).isEqualTo(200);
                gate.releaseLoserAfterWinnerCompletion();
                var b = second.get(20, TimeUnit.SECONDS);
                assertThat(b.statusCode()).isEqualTo(200);
                assertResource(feature, id, json(a), original, false);
                assertResource(feature, id, json(b), original, false);
                assertThat(gate.outcomes.values())
                        .containsExactlyInAnyOrder(
                                org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED,
                                org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED);
                assertThat(stats.getEntityUpdateCount()).isZero();
                assertCommitted(feature, id, original, false, 1L, editable(feature, id, "Loser"));
            } finally {
                gate.releaseAll();
            }
        } finally {
            gate.disarm();
        }
    }

    private Editable editable(String feature, UUID id, String name) {
        if (feature.equals("product")) {
            String value = switch (name) {
                case "Original" -> "12.34";
                case "Winner", "First" -> "45.67";
                default -> "98.76";
            };
            return new Editable(name, value);
        }
        String email = name.equals("Original")
                ? originalEmails.get(id)
                : name.toLowerCase(Locale.ROOT) + "-" + id + "@example.com";
        return new Editable(name, email);
    }

    private java.net.http.HttpResponse<String> mutationRequest(
            String feature, UUID id, String operation, Editable values, String role) throws Exception {
        String body = operation.equals("patch")
                ? "{\"active\":false}"
                : mapper.writeValueAsString(
                        feature.equals("product")
                                ? Map.of("name", values.name(), "price", new java.math.BigDecimal(values.value()))
                                : Map.of("name", values.name(), "email", values.value()));
        var request = java.net.http.HttpRequest.newBuilder(
                        java.net.URI.create("http://127.0.0.1:" + port + "/" + feature + "s/" + id))
                .timeout(java.time.Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("X-Test-Writer", role)
                .method(
                        operation.equals("patch") ? "PATCH" : "PUT",
                        java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    private void assertResource(
            String feature, UUID id, tools.jackson.databind.JsonNode json, Editable expected, boolean active) {
        String field = feature.equals("product") ? "price" : "email";
        assertThat(json.propertyNames()).containsExactlyInAnyOrder("id", "name", field, "active");
        assertThat(json.get("id").asString()).isEqualTo(id.toString());
        assertThat(json.get("name").asString()).isEqualTo(expected.name());
        if (feature.equals("product")) {
            assertThat(json.get(field).decimalValue()).isEqualByComparingTo(expected.value());
        } else {
            assertThat(json.get(field).asString()).isEqualTo(expected.value());
        }
        assertThat(json.get("active").asBoolean()).isEqualTo(active);
    }

    private void assertCommitted(
            String feature, UUID id, Editable expected, boolean active, long version, Editable losing) {
        String field = feature.equals("product") ? "price" : "email";
        var row = jdbc.queryForMap("select id,name," + field + ",active,version from " + feature + "s where id=?", id);
        assertThat(row)
                .containsEntry("id", id)
                .containsEntry("name", expected.name())
                .containsEntry("active", active)
                .containsEntry("version", version)
                .doesNotContainValue(losing.name());
        if (feature.equals("product")) {
            assertThat((java.math.BigDecimal) row.get(field))
                    .isEqualByComparingTo(expected.value())
                    .isNotEqualByComparingTo(losing.value());
        } else {
            assertThat(row).containsEntry(field, expected.value()).doesNotContainValue(losing.value());
        }
    }

    private UUID create(String feature) {
        String originalEmail = "original-" + UUID.randomUUID() + "@example.com";
        return inTransaction(factory, em -> {
            if (feature.equals("product")) {
                var entity = new Product("Original", Money.of("12.34"));
                em.persist(entity);
                em.flush();
                return entity.getId();
            }
            var entity = new Customer("Original", new EmailAddress(originalEmail));
            em.persist(entity);
            em.flush();
            originalEmails.put(entity.getId(), originalEmail);
            return entity.getId();
        });
    }

    private Object find(EntityManager em, String feature, UUID id) {
        return feature.equals("product") ? em.find(Product.class, id) : em.find(Customer.class, id);
    }

    private void mutate(Object value, String mutation, Editable fields) {
        if (value instanceof Product product) {
            if (mutation.equals("patch")) {
                product.deactivate();
            } else {
                product.update(fields.name(), Money.of(fields.value()));
            }
        } else if (value instanceof Customer customer) {
            if (mutation.equals("patch")) {
                customer.deactivate();
            } else {
                customer.update(fields.name(), new EmailAddress(fields.value()));
            }
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Transaction coordination deadline exceeded");
            }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted transaction coordination", failure);
        }
    }

    public static final class VersionSqlInspector implements org.hibernate.resource.jdbc.spi.StatementInspector {
        static final java.util.List<String> statements = new java.util.concurrent.CopyOnWriteArrayList<>();

        @Override
        public String inspect(String sql) {
            statements.add(sql);
            return sql;
        }
    }
    /** Test-only rendezvous after service return, inside the actual outer transaction. */
    static final class CompletionGate {
        private volatile UUID id;
        private volatile CyclicBarrier barrier;
        private volatile boolean ordered;
        private CountDownLatch both;
        private CountDownLatch winnerRelease;
        private CountDownLatch loserRelease;
        private CountDownLatch winnerCompletion;
        final java.util.List<Object> contexts = new CopyOnWriteArrayList<>();
        final Map<String, Integer> outcomes = new ConcurrentHashMap<>();
        final Map<String, String> methods = new ConcurrentHashMap<>();

        void arm(UUID id) {
            this.ordered = false;
            this.barrier = new CyclicBarrier(2);
            contexts.clear();
            outcomes.clear();
            methods.clear();
            this.id = id;
        }

        void armOrdered(UUID id) {
            this.ordered = true;
            both = new CountDownLatch(2);
            winnerRelease = new CountDownLatch(1);
            loserRelease = new CountDownLatch(1);
            winnerCompletion = new CountDownLatch(1);
            contexts.clear();
            outcomes.clear();
            methods.clear();
            this.id = id;
        }

        void disarm() {
            this.id = null;
        }

        void awaitBoth() {
            await(both);
        }

        void releaseWinner() {
            winnerRelease.countDown();
        }

        void releaseLoserAfterWinnerCompletion() {
            await(winnerCompletion);
            assertThat(outcomes)
                    .containsEntry(
                            "winner",
                            org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED);
            loserRelease.countDown();
        }

        void releaseAll() {
            winnerRelease.countDown();
            loserRelease.countDown();
        }

        void assertIndependentContexts() {
            assertThat(contexts).hasSize(2);
            assertThat(contexts.get(0)).isNotSameAs(contexts.get(1));
        }

        void rendezvous(String method, Object[] arguments) {
            if (arguments.length == 0 || !arguments[0].equals(id)) {
                return;
            }
            assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                    .isTrue();
            var holder = TransactionSynchronizationManager.getResourceMap().values().stream()
                    .filter(org.springframework.orm.jpa.EntityManagerHolder.class::isInstance)
                    .map(org.springframework.orm.jpa.EntityManagerHolder.class::cast)
                    .findFirst()
                    .orElseThrow();
            contexts.add(holder.getEntityManager());
            var request = (org.springframework.web.context.request.ServletRequestAttributes)
                    org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
            String role = request.getRequest().getHeader("X-Test-Writer");
            assertThat(role).as("Test request must identify its writer").isNotNull();
            methods.put(role, method);
            boolean orderedSchedule = ordered;
            TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            outcomes.put(role, status);
                            if (orderedSchedule && role.equals("winner")) {
                                winnerCompletion.countDown();
                            }
                        }
                    });
            if (orderedSchedule) {
                both.countDown();
                await(role.equals("winner") ? winnerRelease : loserRelease);
            } else {
                try {
                    barrier.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(failure);
                } catch (BrokenBarrierException | TimeoutException failure) {
                    throw new IllegalStateException("HTTP transaction coordination failed", failure);
                }
            }
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CompletionGateConfiguration {
        @Bean
        static CompletionGate completionGate() {
            return new CompletionGate();
        }

        @Bean
        static BeanPostProcessor completionRendezvous(CompletionGate gate) {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String name) {
                    if (bean instanceof Advised repository
                            && (name.equals("productRepository") || name.equals("customerRepository"))) {
                        // Test-only stale-read fixture: retain optimistic completion assertions after production writes
                        // became serialized.
                        repository.addAdvisor(
                                0,
                                new DefaultPointcutAdvisor(
                                        new StaticMethodMatcherPointcut() {
                                            @Override
                                            public boolean matches(java.lang.reflect.Method method, Class<?> type) {
                                                return method.getName().equals("findLockedById");
                                            }
                                        },
                                        (MethodInterceptor) invocation -> {
                                            if (gate.id != null
                                                    && gate.id.equals(invocation.getArguments()[0])) {
                                                if (bean
                                                        instanceof
                                                        com.example.agenticorderservice.product.persistence
                                                                        .ProductRepository
                                                                products) {
                                                    return products.findById(gate.id);
                                                }
                                                return ((com.example.agenticorderservice.customer.persistence
                                                                        .CustomerRepository)
                                                                bean)
                                                        .findById(gate.id);
                                            }
                                            return invocation.proceed();
                                        }));
                    }
                    if (bean instanceof Advised proxy
                            && (name.equals("productWriteService") || name.equals("customerApplicationService"))) {
                        var advisor = new DefaultPointcutAdvisor(
                                new StaticMethodMatcherPointcut() {
                                    @Override
                                    public boolean matches(java.lang.reflect.Method method, Class<?> type) {
                                        return method.getName().equals("update")
                                                || method.getName().equals("deactivate");
                                    }
                                },
                                (MethodInterceptor) invocation -> {
                                    var result = invocation.proceed();
                                    gate.rendezvous(invocation.getMethod().getName(), invocation.getArguments());
                                    return result;
                                });
                        var advisors = proxy.getAdvisors();
                        for (int i = 0; i < advisors.length; i++) {
                            if (advisors[i].getAdvice() instanceof TransactionInterceptor) {
                                proxy.addAdvisor(i + 1, advisor);
                                break;
                            }
                        }
                    }
                    return bean;
                }
            };
        }
    }
}
