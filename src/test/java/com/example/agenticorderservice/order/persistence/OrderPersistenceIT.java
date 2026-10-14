package com.example.agenticorderservice.order.persistence;

import com.example.agenticorderservice.order.domain.Order;
import com.example.agenticorderservice.order.domain.OrderLineInput;
import com.example.agenticorderservice.shared.domain.Money;
import com.example.agenticorderservice.product.domain.Product;
import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.stream.Collectors;
import org.hibernate.SessionFactory;
import java.util.List;
import java.util.UUID;
import com.example.agenticorderservice.PostgreSqlTestConfiguration;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import static com.example.agenticorderservice.PostgreSqlTestConfiguration.inTransaction;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("integration-test")
@Import(PostgreSqlTestConfiguration.class)
class OrderPersistenceIT {
    @Autowired EntityManagerFactory factory;
    @Autowired OrderRepository repository;
    @Autowired JdbcTemplate jdbc;

    private UUID customer() {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into customers (id, name, email, active, version) values (?, 'fixture', ?, true, 0)", id, id + "@example.com"); return id;
    }
    private UUID product() {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into products (id, name, price, active, version) values (?, 'original', 999999.99, true, 0)", id); return id;
    }
    @Test void cascadesOnlyRootAndReloadsOriginalLargeSnapshotsWithExplicitFetch() {
        UUID customer = customer(), first = product(), second = product();
        var order = new Order(customer, List.of(new OrderLineInput(first, "original", Money.of("999999.99"), 10000),
                new OrderLineInput(second, "original", Money.of("999999.99"), 10000)));
        UUID id = inTransaction(factory, em -> {
            em.persist(order); em.flush();
            assertThat(order.getId()).isNotNull();
            assertThat(order.getItems()).allSatisfy(item -> {
                assertThat(item.getId()).isNotNull(); assertThat(item.getOrder()).isSameAs(order);
            });
            em.clear();
            var lazy = em.find(Order.class, order.getId());
            assertThat(factory.getPersistenceUnitUtil().isLoaded(lazy, "items")).isFalse();
            return order.getId();
        });
        assertThat(jdbc.queryForObject("select count(*) from order_items where order_id = ?", Long.class, id)).isEqualTo(2);
        jdbc.update("update products set name='changed', price=1.00, active=false where id in (?, ?)", first, second);
        var stats = factory.unwrap(SessionFactory.class).getStatistics(); stats.clear();
        Order detached = repository.findByIdWithItems(id).orElseThrow();
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        assertThat(factory.getPersistenceUnitUtil().isLoaded(detached, "items")).isTrue();
        assertThat(detached.getSubtotal()).isEqualTo(Money.of("19999999800.00"));
        assertThat(detached.getDiscount()).isEqualTo(Money.of("1999999980.00"));
        assertThat(detached.getTotal()).isEqualTo(Money.of("17999999820.00"));
        assertThat(detached.getItems()).allSatisfy(item -> {
            assertThat(item.getOrder().getId()).isEqualTo(id);
            assertThat(item.getProductName()).isEqualTo("original");
            assertThat(item.getUnitPrice()).isEqualTo(Money.of("999999.99"));
            assertThat(item.getLineTotal()).isEqualTo(Money.of("9999999900.00"));
        });
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        assertThatThrownBy(() -> inTransaction(factory, em -> { em.remove(em.find(Order.class, id)); em.flush(); return null; }))
                .hasStackTraceContaining("fk_order_items_order");
        assertThat(jdbc.queryForObject("select count(*) from order_items where order_id = ?", Long.class, id)).isEqualTo(2);
    }
    // PRD AC-007/AC-009/AC-015: production root persistence and immutable closed-context reads.
    @Test void repositorySaveUsesGeneratedIdentityAndFetchesMultipleAggregatesWithoutItemQueries() {
        UUID customer = customer(), first = product(), second = product();
        var roots = List.of(
                new Order(customer, List.of(new OrderLineInput(first, "first", Money.of("1"), 2),
                        new OrderLineInput(second, "second", Money.of("2"), 3))),
                new Order(customer, List.of(new OrderLineInput(first, "first", Money.of("1"), 4),
                        new OrderLineInput(second, "second", Money.of("2"), 5))));
        for (Order root : roots) {
            int hashBeforePersistence = root.hashCode();
            assertThat(repository.save(root)).isSameAs(root);
            assertThat(root.getId()).isNotNull();
            assertThat(root.hashCode()).isEqualTo(hashBeforePersistence);
            assertThat(root.getItems()).allSatisfy(item -> assertThat(item.getId()).isNotNull());
        }
        var stats = factory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        for (Order root : roots) {
            assertThat(repository.findByIdWithItems(root.getId()).orElseThrow().getItems()).hasSize(2);
        }
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);

        stats.clear();
        List<Snapshot> values = inTransaction(factory, em -> {
            Order unloadedProxy = em.getReference(Order.class, roots.getFirst().getId());
            assertThat(factory.getPersistenceUnitUtil().isLoaded(unloadedProxy)).isFalse();
            unloadedProxy.hashCode();
            assertThat(unloadedProxy.equals(unloadedProxy)).isTrue();
            assertThat(unloadedProxy.getId()).isEqualTo(roots.getFirst().getId());
            assertThat(factory.getPersistenceUnitUtil().isLoaded(unloadedProxy)).isFalse();
            assertThat(stats.getPrepareStatementCount()).isZero();
            return roots.stream().map(root -> {
            Order loaded = em.createQuery(
                            "select distinct o from PurchaseOrder o left join fetch o.items where o.id = :id", Order.class)
                    .setParameter("id", root.getId()).getSingleResult();
            var proxy = em.getReference(Order.class, root.getId());
            long statements = stats.getPrepareStatementCount();
            proxy.hashCode();
            assertThat(proxy.equals(loaded)).isTrue();
            assertThat(stats.getPrepareStatementCount()).isEqualTo(statements);
            return new Snapshot(loaded.getId(), loaded.getTotal(), loaded.getItems().stream()
                    .map(item -> new ItemSnapshot(item.getProductName(), item.getQuantity(), item.getLineTotal())).toList());
            }).toList();
        });
        assertThat(values).extracting(Snapshot::total).containsExactly(Money.of("8"), Money.of("14"));
        assertThat(values).allSatisfy(value -> {
            assertThat(value.items()).hasSize(2);
            assertThat(value.items()).extracting(ItemSnapshot::name).containsExactlyInAnyOrder("first", "second");
        });
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
    }

    private record Snapshot(UUID id, Money total, List<ItemSnapshot> items) { }
    private record ItemSnapshot(String name, int quantity, Money lineTotal) { }

    @Test void rejectsDuplicateItemsInvalidChildValuesAndReferencedDeletion() {
        UUID customer = customer(), product = product(), other = product();
        var order = new Order(customer, List.of(new OrderLineInput(product, "original", Money.of("1"), 1)));
        inTransaction(factory, em -> { em.persist(order); return null; });
        assertThatThrownBy(() -> jdbc.update("insert into order_items values (?, ?, ?, 'original', 1, 1.00, 1.00)",
                UUID.randomUUID(), order.getId(), product)).hasStackTraceContaining("uq_order_items_product");
        for (int quantity : new int[]{0, -1, 10001}) {
            assertThatThrownBy(() -> jdbc.update("insert into order_items values (?, ?, ?, 'original', ?, 1.00, 1.00)",
                    UUID.randomUUID(), order.getId(), other, quantity)).hasStackTraceContaining("ck_order_items_quantity");
        }
        for (String invalid : new String[]{"-0.01", "1000000", "1.001", "10.000", "NaN", "Infinity", "-Infinity"}) {
            assertThatThrownBy(() -> jdbc.update("insert into order_items values (?, ?, ?, 'original', 1, cast(? as numeric), 1.00)",
                    UUID.randomUUID(), order.getId(), other, invalid)).hasStackTraceContaining("ck_order_items_unit_price");
        }
        for (String invalid : new String[]{"-0.01", "1.001", "NaN", "Infinity", "-Infinity"}) {
            assertThatThrownBy(() -> jdbc.update("insert into order_items values (?, ?, ?, 'original', 1, 1.00, cast(? as numeric))",
                    UUID.randomUUID(), order.getId(), other, invalid)).hasStackTraceContaining("ck_order_items_line_total");
        }
        assertThatThrownBy(() -> jdbc.update("delete from customers where id=?", customer)).hasStackTraceContaining("fk_orders_customer");
        assertThatThrownBy(() -> jdbc.update("delete from products where id=?", product)).hasStackTraceContaining("fk_order_items_product");
        assertThatThrownBy(() -> jdbc.update("insert into order_items values (?, ?, ?, 'original', 1, 1.00, 1.00)",
                UUID.randomUUID(), UUID.randomUUID(), other)).hasStackTraceContaining("fk_order_items_order");
        assertThatThrownBy(() -> jdbc.update("insert into order_items values (?, null, ?, 'original', 1, 1.00, 1.00)",
                UUID.randomUUID(), other)).hasStackTraceContaining("order_id");
        assertThatThrownBy(() -> jdbc.update("insert into order_items values (?, ?, null, 'original', 1, 1.00, 1.00)",
                UUID.randomUUID(), order.getId())).hasStackTraceContaining("product_id");
        assertThat(jdbc.queryForObject("select count(*) from order_items where order_id=?", Long.class, order.getId())).isEqualTo(1);
    }

    @Test void confirmedRollbackAfterRootFlushLeavesNoPartialAggregate() {
        UUID customer = customer(), valid = product(), missing = UUID.randomUUID();
        var order = new Order(customer, List.of(new OrderLineInput(valid, "original", Money.of("1"), 1)));
        assertThatThrownBy(() -> inTransaction(factory, em -> {
            em.persist(order); em.flush();
            em.createNativeQuery("insert into order_items values (:id, :orderId, :productId, 'bad', 1, 1.00, 1.00)")
                    .setParameter("id", UUID.randomUUID()).setParameter("orderId", order.getId())
                    .setParameter("productId", missing).executeUpdate(); return null;
        })).hasStackTraceContaining("fk_order_items_product");
        assertThat(jdbc.queryForObject("select count(*) from orders where id = ?", Long.class, order.getId())).isZero();
        assertThat(jdbc.queryForObject("select count(*) from order_items where order_id = ?", Long.class, order.getId())).isZero();
        assertThat(jdbc.queryForObject("select count(*) from products where id = ?", Long.class, valid)).isEqualTo(1);
    }

    // Finding 2: BR-010/AC-009/AC-015; all snapshot values are captured inside a transaction.
    @Test
    void repeatedlyMaterializesOriginalSnapshotsAfterManagedCatalogMutations() {
        UUID customerId = customer();
        var first = new Product("Original", Money.of("25.00"));
        var second = new Product("Second", Money.of("50.05"));
        HistoricalOrder initial = inTransaction(factory, em -> {
            em.persist(first);
            em.persist(second);
            var root = new Order(customerId, List.of(
                    new OrderLineInput(first.getId(), first.getName(), first.getPrice(), 2),
                    new OrderLineInput(second.getId(), second.getName(), second.getPrice(), 1)));
            em.persist(root); // Root cascade alone persists items.
            em.flush();
            em.clear();
            return historicalOrder(em, root.getId());
        });
        HistoricalOrder expected = new HistoricalOrder(initial.id(), customerId,
                Money.of("100.05"), Money.of("10.01"), Money.of("90.04"), Map.of(
                first.getId(), new HistoricalItem("Original", Money.of("25.00"), 2, Money.of("50.00")),
                second.getId(), new HistoricalItem("Second", Money.of("50.05"), 1, Money.of("50.05"))));
        assertThat(initial).isEqualTo(expected);

        inTransaction(factory, em -> {
            em.find(Product.class, first.getId()).update("Renamed", Money.of("25.00"));
            em.flush();
            em.clear();
            assertThat(em.find(Product.class, first.getId()).getName()).isEqualTo("Renamed");
            assertThat(historicalOrder(em, initial.id())).isEqualTo(expected);
            return null;
        });
        inTransaction(factory, em -> {
            em.find(Product.class, first.getId()).update("Renamed", Money.of("40.00"));
            em.find(Product.class, second.getId()).update("Second changed", Money.zero());
            em.flush();
            em.clear();
            assertThat(em.find(Product.class, first.getId()).getPrice()).isEqualTo(Money.of("40.00"));
            assertThat(em.find(Product.class, second.getId()).getName()).isEqualTo("Second changed");
            assertThat(em.find(Product.class, second.getId()).getPrice()).isEqualTo(Money.zero());
            assertThat(historicalOrder(em, initial.id())).isEqualTo(expected);
            return null;
        });
        inTransaction(factory, em -> {
            em.find(Product.class, first.getId()).deactivate();
            em.find(Product.class, second.getId()).deactivate();
            em.flush();
            em.clear();
            assertThat(em.find(Product.class, first.getId()).isActive()).isFalse();
            assertThat(em.find(Product.class, second.getId()).isActive()).isFalse();
            assertThat(historicalOrder(em, initial.id())).isEqualTo(expected);
            return null;
        });
        var stats = factory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        HistoricalOrder firstRead = inTransaction(factory, em -> historicalOrder(em, initial.id()));
        HistoricalOrder repeatedRead = inTransaction(factory, em -> historicalOrder(em, initial.id()));
        assertThat(firstRead).isEqualTo(expected);
        assertThat(repeatedRead).isEqualTo(expected);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
        // Both contexts are closed. These are immutable values, never entity responses.
        assertThat(firstRead.items().get(first.getId()).name()).isEqualTo("Original");
        assertThat(firstRead.items().get(first.getId()).unitPrice()).isEqualTo(Money.of("25.00"));
        assertThat(firstRead.items().get(first.getId()).quantity()).isEqualTo(2);
        assertThat(firstRead.items().get(first.getId()).lineTotal()).isEqualTo(Money.of("50.00"));
        assertThat(firstRead.subtotal()).isEqualTo(Money.of("100.05"));
        assertThat(firstRead.discount()).isEqualTo(Money.of("10.01"));
        assertThat(firstRead.total()).isEqualTo(Money.of("90.04"));
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
    }

    // Finding 3: persistence stores trusted historical facts; it does not re-read/reconcile the catalog.
    @Test
    void preservesTrustedCarrierFactsRatherThanReplacingThemWithCurrentCatalogValues() {
        UUID customerId = customer(), productId = product();
        var order = new Order(customerId, List.of(
                new OrderLineInput(productId, "Captured earlier", Money.of("25.00"), 2)));
        repository.save(order);
        var snapshot = inTransaction(factory, em -> historicalOrder(em, order.getId()));
        assertThat(snapshot.items()).containsExactlyEntriesOf(Map.of(productId,
                new HistoricalItem("Captured earlier", Money.of("25.00"), 2, Money.of("50.00"))));
        assertThat(snapshot.subtotal()).isEqualTo(Money.of("50.00"));
        assertThat(snapshot.discount()).isEqualTo(Money.zero());
        assertThat(snapshot.total()).isEqualTo(Money.of("50.00"));
        assertThat(jdbc.queryForObject("select name from products where id=?", String.class, productId)).isEqualTo("original");
        assertThat(jdbc.queryForObject("select price from products where id=?", java.math.BigDecimal.class, productId))
                .isEqualByComparingTo("999999.99");
        // Existence/active-state/current-facts validation belongs to later Order Placement, not this carrier.
    }

    private HistoricalOrder historicalOrder(EntityManager em, UUID id) {
        Order root = em.createQuery(
                        "select distinct o from PurchaseOrder o left join fetch o.items where o.id = :id", Order.class)
                .setParameter("id", id).getSingleResult();
        assertThat(root.getId()).isEqualTo(id);
        assertThat(root.getItems()).allSatisfy(item -> {
            assertThat(item.getId()).isNotNull();
            assertThat(item.getOrder().getId()).isEqualTo(id);
        });
        return new HistoricalOrder(root.getId(), root.getCustomerId(), root.getSubtotal(), root.getDiscount(), root.getTotal(),
                root.getItems().stream().collect(Collectors.toUnmodifiableMap(
                        item -> item.getProductId(), item -> new HistoricalItem(
                                item.getProductName(), item.getUnitPrice(), item.getQuantity(), item.getLineTotal()))));
    }

    private record HistoricalOrder(UUID id, UUID customerId, Money subtotal, Money discount, Money total,
                                   Map<UUID, HistoricalItem> items) { }
    private record HistoricalItem(String name, Money unitPrice, int quantity, Money lineTotal) { }
}
