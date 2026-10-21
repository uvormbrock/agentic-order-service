package com.example.agenticorderservice.customer.application;

import static org.assertj.core.api.Assertions.*;

import com.example.agenticorderservice.order.application.OrderReferenceQuery;
import com.example.agenticorderservice.shared.api.PostgreSqlApiTestSupport;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

class CustomerTransactionParticipationIT extends PostgreSqlApiTestSupport {
    @Autowired
    com.example.agenticorderservice.customer.application.port.in.CustomerFacts facts;

    @Autowired
    OrderReferenceQuery references;

    @Autowired
    com.example.agenticorderservice.customer.application.port.in.CustomerOperations customers;

    @Autowired
    PlatformTransactionManager manager;

    @Test
    void crossFeatureProvidersRequireCallerTransactionEvenForEmptyReads() {
        UUID absent = UUID.randomUUID();
        assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() -> facts.lock(absent));
        assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() -> facts.current(List.of()));
        assertThatExceptionOfType(IllegalTransactionStateException.class)
                .isThrownBy(() -> references.references(absent));
        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                .isFalse();
    }

    @Test
    void factsAndDeletionJoinCallerTransactionAndRollbackPreservesCustomer() {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into customers(id,name,email,active,version) values (?,'Original',?,true,0)",
                id,
                id + "@example.com");
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            Object ambient = TransactionSynchronizationManager.getResource(factory);
            assertThat(ambient).isNotNull();
            var locked = facts.lock(id);
            assertThat(locked.id()).isEqualTo(id);
            var current = facts.current(List.of(id));
            assertThat(current).containsEntry(id, locked);
            assertThatThrownBy(current::clear).isInstanceOf(UnsupportedOperationException.class);
            assertThat(references.references(id)).isFalse();
            customers.delete(id);
            ((org.springframework.orm.jpa.EntityManagerHolder) ambient)
                    .getEntityManager()
                    .flush();
            assertThat(TransactionSynchronizationManager.getResource(factory)).isSameAs(ambient);
            assertThat(status.isCompleted()).isFalse();
            status.setRollbackOnly();
        });
        assertThat(jdbc.queryForMap("select name,active,version from customers where id=?", id))
                .containsEntry("name", "Original")
                .containsEntry("active", true)
                .containsEntry("version", 0L);
        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                .isFalse();
    }
}
