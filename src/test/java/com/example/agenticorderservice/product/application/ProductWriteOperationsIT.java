package com.example.agenticorderservice.product.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.PostgreSqlTestConfiguration;
import com.example.agenticorderservice.product.application.port.in.ProductWriteOperations;
import com.example.agenticorderservice.product.domain.Product;
import com.example.agenticorderservice.product.persistence.ProductWriteAdapter;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("integration-test")
@Import(PostgreSqlTestConfiguration.class)
class ProductWriteOperationsIT {
    @Autowired
    ProductWriteOperations writes;

    @Autowired
    ProductApplicationService facade;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager manager;

    @Autowired
    EntityManagerFactory factory;

    @MockitoSpyBean
    ProductWriteAdapter adapter;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void directAndCompatibilityWritesOwnReadWriteTransactionsAndCommitManagedMutations(boolean compatibility) {
        observeWriteTransaction(null);
        var created = create(compatibility, "Original");
        assertThat(created.id()).isNotNull();
        update(compatibility, created.id());
        deactivate(compatibility, created.id());
        assertThat(jdbc.queryForMap("select name,price,active from products where id=?", created.id()))
                .containsEntry("name", "Changed")
                .containsEntry("price", new BigDecimal("45.67"))
                .containsEntry("active", false);
        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                .isFalse();
        verify(adapter).save(any(Product.class));
        verify(adapter, times(2)).findLockedById(created.id());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void directAndCompatibilityWritesJoinAmbientTransactionAndAllRollBack(boolean compatibility) {
        UUID existing = UUID.randomUUID();
        jdbc.update(
                "insert into products(id,name,price,active,version) values (?, 'Original', 12.34, true, 0)", existing);
        UUID[] created = new UUID[1];
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            Object ambient = TransactionSynchronizationManager.getResource(factory);
            assertThat(ambient).isNotNull();
            observeWriteTransaction(ambient);
            created[0] = create(compatibility, "Rolled back").id();
            update(compatibility, existing);
            deactivate(compatibility, existing);
            ((org.springframework.orm.jpa.EntityManagerHolder) ambient)
                    .getEntityManager()
                    .flush();
            assertThat(TransactionSynchronizationManager.getResource(factory)).isSameAs(ambient);
            assertThat(status.isCompleted()).isFalse();
            status.setRollbackOnly();
        });
        assertThat(jdbc.queryForObject("select count(*) from products where id=?", Integer.class, created[0]))
                .isZero();
        assertThat(jdbc.queryForMap("select name,price,active,version from products where id=?", existing))
                .containsEntry("name", "Original")
                .containsEntry("price", new BigDecimal("12.34"))
                .containsEntry("active", true)
                .containsEntry("version", 0L);
        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                .isFalse();
    }

    private void observeWriteTransaction(Object expectedResource) {
        org.mockito.stubbing.Answer<Object> observation = call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                    .isTrue();
            assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly())
                    .isFalse();
            assertThat(jdbc.queryForObject("show transaction_read_only", String.class))
                    .isEqualTo("off");
            if (expectedResource != null) {
                assertThat(TransactionSynchronizationManager.getResource(factory))
                        .isSameAs(expectedResource);
            }
            return call.callRealMethod();
        };
        doAnswer(observation).when(adapter).save(any(Product.class));
        doAnswer(observation).when(adapter).findLockedById(any(UUID.class));
    }

    private ProductResult create(boolean compatibility, String name) {
        return compatibility
                ? facade.create(name, new BigDecimal("12.34"))
                : writes.create(name, new BigDecimal("12.34"));
    }

    private void update(boolean compatibility, UUID id) {
        if (compatibility) facade.update(id, "Changed", new BigDecimal("45.67"));
        else writes.update(id, "Changed", new BigDecimal("45.67"));
    }

    private void deactivate(boolean compatibility, UUID id) {
        if (compatibility) facade.deactivate(id);
        else writes.deactivate(id);
    }
}
