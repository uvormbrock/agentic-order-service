package com.example.agenticorderservice.order.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.PostgreSqlTestConfiguration;
import com.example.agenticorderservice.customer.persistence.CustomerReadAdapter;
import jakarta.persistence.EntityManagerFactory;
import java.util.Set;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("integration-test")
@Import(PostgreSqlTestConfiguration.class)
class OrderReadQueryIT {
    @Autowired
    com.example.agenticorderservice.order.application.port.in.OrderQueries query;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManagerFactory factory;

    @Autowired
    PlatformTransactionManager transactionManager;

    @MockitoSpyBean
    CustomerReadAdapter customers;

    private UUID storedOrder(UUID customer, UUID product) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into orders(id,customer_id,subtotal,discount,total) values (?,?,123.45,3.21,120.24)",
                id,
                customer);
        jdbc.update(
                "insert into order_items(id,order_id,product_id,product_name,quantity,unit_price,line_total) values (?,?,?,'Captured',2,10.00,123.45)",
                UUID.randomUUID(),
                id,
                product);
        return id;
    }

    private UUID customer() {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into customers(id,name,email,active,version) values (?,'Current',?,false,0)",
                id,
                id + "@example.com");
        return id;
    }

    private UUID product() {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into products(id,name,price,active,version) values (?,'Changed',999.99,false,0)", id);
        return id;
    }

    @Test
    void readsStoredAmountsWithoutRecalculationAndMaterializesInsideReadOnlyTransaction() {
        UUID customer = customer(), id = storedOrder(customer, product());
        doAnswer(call -> {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                            .isTrue();
                    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly())
                            .isTrue();
                    assertThat(jdbc.queryForObject("show transaction_read_only", String.class))
                            .isEqualTo("on");
                    return call.callRealMethod();
                })
                .when(customers)
                .findCurrentByIds(Set.of(customer));
        var stats = factory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        OrderResult result = query.get(id);
        // The diagnostic SHOW above is JDBC-only and is not part of Hibernate's query count.
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                .isFalse();
        assertThat(TransactionSynchronizationManager.getResource(factory)).isNull();
        assertThat(result.customer().name()).isEqualTo("Current");
        assertThat(result.customer().active()).isFalse();
        assertThat(result.currency()).isEqualTo("EUR");
        assertThat(result.subtotal()).isEqualByComparingTo("123.45");
        assertThat(result.discount()).isEqualByComparingTo("3.21");
        assertThat(result.total()).isEqualByComparingTo("120.24");
        var item = result.items().getFirst();
        assertThat(item.productName()).isEqualTo("Captured");
        assertThat(item.quantity()).isEqualTo(2);
        assertThat(item.unitPrice()).isEqualByComparingTo("10.00");
        assertThat(item.lineTotal()).isEqualByComparingTo("123.45");
        assertThatThrownBy(result.items()::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    void readJoinsExistingReadWriteTransactionWithoutReplacingPersistenceContext() {
        UUID customer = customer(), id = storedOrder(customer, product());
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Object ambientResource = TransactionSynchronizationManager.getResource(factory);
            assertThat(ambientResource).isNotNull();
            doAnswer(call -> {
                        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                                .isTrue();
                        assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly())
                                .isFalse();
                        assertThat(TransactionSynchronizationManager.getResource(factory))
                                .isSameAs(ambientResource);
                        assertThat(jdbc.queryForObject("show transaction_read_only", String.class))
                                .isEqualTo("off");
                        return call.callRealMethod();
                    })
                    .when(customers)
                    .findCurrentByIds(Set.of(customer));
            assertThat(query.get(id).id()).isEqualTo(id);
            assertThat(query.list()).extracting(OrderResult::id).contains(id);
            assertThat(TransactionSynchronizationManager.getResource(factory)).isSameAs(ambientResource);
            assertThat(status.isCompleted()).isFalse();
            assertThat(status.isRollbackOnly()).isFalse();
        });
        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                .isFalse();
        assertThat(TransactionSynchronizationManager.getResource(factory)).isNull();
    }

    @Test
    void listsMultipleRootsWithMultipleItemsInTwoQueriesAndSerializesAfterTransaction() {
        UUID customer = customer(), firstProduct = product(), secondProduct = product();
        UUID first = storedOrder(customer, firstProduct), second = storedOrder(customer, firstProduct);
        for (UUID id : java.util.List.of(first, second)) {
            jdbc.update(
                    "insert into order_items(id,order_id,product_id,product_name,quantity,unit_price,line_total) values (?,?,?,'Second captured',3,7.00,21.00)",
                    UUID.randomUUID(),
                    id,
                    secondProduct);
        }
        var stats = factory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        var results = query.list();
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                .isFalse();
        assertThat(TransactionSynchronizationManager.getResource(factory)).isNull();
        var selected = results.stream()
                .filter(value -> Set.of(first, second).contains(value.id()))
                .toList();
        assertThat(selected).extracting(OrderResult::id).containsExactlyInAnyOrder(first, second);
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        for (var result : selected) {
            assertThat(result.items()).hasSize(2);
            assertThat(result.items())
                    .extracting(OrderResult.Item::productId)
                    .containsExactlyInAnyOrder(firstProduct, secondProduct);
            var response = new com.example.agenticorderservice.order.api.OrderResponse(
                    result.id(),
                    result.customerId(),
                    result.customer(),
                    result.items(),
                    result.currency(),
                    result.subtotal(),
                    result.discount(),
                    result.total());
            var json = mapper.readTree(mapper.writeValueAsString(response));
            assertThat(json.get("id").asString()).isEqualTo(result.id().toString());
            assertThat(json.get("items").size()).isEqualTo(2);
            assertThat(json.get("total").decimalValue()).isEqualByComparingTo("120.24");
        }
        assertThatThrownBy(results::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
        stats.clear();
        assertThat(query.get(first).items()).hasSize(2);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
    }
}
