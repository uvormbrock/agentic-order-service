package com.example.agenticorderservice.product.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.PostgreSqlTestConfiguration;
import com.example.agenticorderservice.product.application.port.in.GetProductQuery;
import com.example.agenticorderservice.product.persistence.ProductReadAdapter;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest
@ActiveProfiles("integration-test")
@Import(PostgreSqlTestConfiguration.class)
class GetProductQueryIT {
    @Autowired
    GetProductQuery query;

    @Autowired
    com.example.agenticorderservice.product.application.port.in.ListProductsQuery listQuery;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ProductApplicationService facade;

    @Autowired
    org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Autowired
    jakarta.persistence.EntityManagerFactory entityManagerFactory;

    @MockitoSpyBean
    ProductReadAdapter adapter;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void materializesImmutableFactsInsideApplicationOwnedReadOnlyTransaction(boolean active) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into products(id,name,price,active,version) values (?, ?, 12.34, ?, 0)", id, "Äpfel😀", active);
        doAnswer(call -> {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                            .isTrue();
                    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly())
                            .isTrue();
                    assertThat(jdbc.queryForObject("show transaction_read_only", String.class))
                            .isEqualTo("on");
                    return call.callRealMethod();
                })
                .when(adapter)
                .findById(id);
        var result = query.get(id);
        verify(adapter).findById(id);
        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                .isFalse();
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.name()).isEqualTo("Äpfel😀");
        assertThat(result.price()).isEqualByComparingTo("12.34");
        assertThat(result.active()).isEqualTo(active);
        jdbc.update("update products set name='Changed',price=99.99,active=? where id=?", !active, id);
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.name()).isEqualTo("Äpfel😀");
        assertThat(result.price()).isEqualByComparingTo("12.34");
        assertThat(result.active()).isEqualTo(active);
    }

    @Test
    void queryAndCompatibilityFacadeJoinExistingReadWriteTransaction() {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into products(id,name,price,active,version) values (?, ?, 12.34, true, 0)",
                id,
                "Ambient fixture");
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        transaction.setReadOnly(false);
        transaction.executeWithoutResult(status -> {
            Object ambientResource = TransactionSynchronizationManager.getResource(entityManagerFactory);
            assertThat(ambientResource).isNotNull();
            doAnswer(call -> {
                        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                                .isTrue();
                        assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly())
                                .isFalse();
                        assertThat(TransactionSynchronizationManager.getResource(entityManagerFactory))
                                .isSameAs(ambientResource);
                        assertThat(jdbc.queryForObject("show transaction_read_only", String.class))
                                .isEqualTo("off");
                        return call.callRealMethod();
                    })
                    .when(adapter)
                    .findById(id);
            var direct = query.get(id);
            assertThat(direct.id()).isEqualTo(id);
            assertThat(facade.get(id)).isEqualTo(direct);
            verify(adapter, times(2)).findById(id);
            assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                    .isTrue();
            assertThat(TransactionSynchronizationManager.getResource(entityManagerFactory))
                    .isSameAs(ambientResource);
            assertThat(status.isCompleted()).isFalse();
            assertThat(status.isRollbackOnly()).isFalse();
        });
        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                .isFalse();
        assertThat(TransactionSynchronizationManager.getResource(entityManagerFactory))
                .isNull();
    }

    @Test
    void missingLookupEndsTransactionAndRetainsFeatureException() {
        assertThatThrownBy(() -> query.get(UUID.randomUUID())).isInstanceOf(ProductNotFoundException.class);
        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                .isFalse();
    }

    @Test
    void listMaterializesImmutableFactsInsideOwnedReadOnlyTransaction() {
        UUID active = UUID.randomUUID(), inactive = UUID.randomUUID();
        jdbc.update("insert into products(id,name,price,active,version) values (?,'List active',12.34,true,0)", active);
        jdbc.update(
                "insert into products(id,name,price,active,version) values (?,'List inactive',4.56,false,0)", inactive);
        doAnswer(call -> {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                            .isTrue();
                    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly())
                            .isTrue();
                    assertThat(jdbc.queryForObject("show transaction_read_only", String.class))
                            .isEqualTo("on");
                    return call.callRealMethod();
                })
                .when(adapter)
                .findAll();
        var results = listQuery.list();
        verify(adapter).findAll();
        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                .isFalse();
        var relevant = results.stream()
                .filter(value -> value.id().equals(active) || value.id().equals(inactive))
                .toList();
        assertThat(relevant)
                .containsExactlyInAnyOrder(
                        new ProductResult(active, "List active", new java.math.BigDecimal("12.34"), true),
                        new ProductResult(inactive, "List inactive", new java.math.BigDecimal("4.56"), false));
        assertThatThrownBy(results::clear).isInstanceOf(UnsupportedOperationException.class);
        jdbc.update("update products set name='Changed',price=99.99,active=false where id=?", active);
        assertThat(relevant)
                .contains(new ProductResult(active, "List active", new java.math.BigDecimal("12.34"), true));
    }
}
