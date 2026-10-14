package com.example.agenticorderservice.customer.persistence;

import com.example.agenticorderservice.customer.domain.Customer;
import com.example.agenticorderservice.customer.domain.EmailAddress;
import java.util.UUID;
import com.example.agenticorderservice.PostgreSqlTestConfiguration;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
// Accepted D1/D3/D7/D9 and PRD inactive-inclusive email uniqueness.
class CustomerPersistenceIT {
    @Autowired EntityManagerFactory factory;
    @Autowired CustomerRepository repository;
    @Autowired JdbcTemplate jdbc;

    @Test void roundTripsEmailAndUnicodeThenRejectsInactiveDuplicate() {
        String email = UUID.randomUUID() + "+tag@example.com";
        var original = new Customer("  Älice😀  ", new EmailAddress(" " + email.toUpperCase(java.util.Locale.ROOT) + " "));
        int hash = original.hashCode();
        UUID id = inTransaction(factory, em -> {
            em.persist(original); em.flush(); em.clear();
            var loaded = em.find(Customer.class, original.getId());
            assertThat(loaded.getName()).isEqualTo("Älice😀");
            assertThat(loaded.getEmail()).isEqualTo(new EmailAddress(email));
            loaded.deactivate(); return loaded.getId();
        });
        assertThat(id).isNotNull(); assertThat(original.hashCode()).isEqualTo(hash);
        assertThat(repository.findById(id).orElseThrow().isActive()).isFalse();
        assertThatThrownBy(() -> inTransaction(factory, em -> {
            em.persist(new Customer("duplicate", new EmailAddress(email))); em.flush(); return null;
        })).hasStackTraceContaining("uq_customers_email");
        assertThat(jdbc.queryForObject("select count(*) from customers where lower(email) = ?", Long.class, email)).isEqualTo(1);
    }

    // Finding 4: accepted D1/D3, BR-011/BR-012; valid/invalid managed updates after a real insert.
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void retainsGeneratedIdentityAndBothFieldsThroughApprovedAndRejectedUpdates(boolean inactive) {
        var original = new Customer("Original", new EmailAddress(UUID.randomUUID() + "@example.com"));
        assertThat(original.getId()).isNull();
        var changedEmail = new EmailAddress(UUID.randomUUID() + "+changed@example.com");
        UUID id = inTransaction(factory, em -> {
            em.persist(original);
            em.flush();
            UUID assignedId = original.getId();
            assertThat(assignedId).isNotNull();
            em.clear();
            var loaded = em.find(Customer.class, assignedId);
            if (inactive) { loaded.deactivate(); }
            loaded.update("\u2003 Älice😀 ", changedEmail);
            em.flush();
            em.clear();
            var reloaded = em.find(Customer.class, assignedId);
            assertThat(reloaded.getId()).isEqualTo(assignedId);
            assertThat(reloaded.getName()).isEqualTo("Älice😀");
            assertThat(reloaded.getEmail()).isEqualTo(changedEmail);
            assertThat(reloaded.isActive()).isEqualTo(!inactive);
            assertThatNullPointerException().isThrownBy(() -> reloaded.update("Unwanted name", null));
            assertThatIllegalArgumentException().isThrownBy(() -> reloaded.update("", new EmailAddress("unwanted@example.com")));
            assertThat(reloaded.getName()).isEqualTo("Älice😀");
            assertThat(reloaded.getEmail()).isEqualTo(changedEmail);
            assertThat(reloaded.isActive()).isEqualTo(!inactive);
            assertThat(reloaded.getId()).isEqualTo(assignedId);
            em.flush();
            em.clear();
            var afterRejectedUpdates = em.find(Customer.class, assignedId);
            assertThat(afterRejectedUpdates.getId()).isEqualTo(assignedId);
            assertThat(afterRejectedUpdates.getName()).isEqualTo("Älice😀");
            assertThat(afterRejectedUpdates.getEmail()).isEqualTo(changedEmail);
            assertThat(afterRejectedUpdates.isActive()).isEqualTo(!inactive);
            return assignedId;
        });
        var committed = repository.findById(id).orElseThrow();
        assertThat(committed.getId()).isEqualTo(id);
        assertThat(committed.getName()).isEqualTo("Älice😀");
        assertThat(committed.getEmail()).isEqualTo(changedEmail);
        assertThat(committed.isActive()).isEqualTo(!inactive);
    }

    @Test
    void roundTripsMaximumNameAndEmailAndRejectsOverlongStoredEmail() {
        String name = "😀".repeat(199) + "Ä";
        String normalizedEmail = "a".repeat(242) + "@example.com";
        var customer = new Customer("\u2003" + name + " ", new EmailAddress(" " + normalizedEmail.toUpperCase(java.util.Locale.ROOT) + "\t"));
        UUID id = inTransaction(factory, em -> {
            em.persist(customer);
            em.flush();
            em.clear();
            var reloaded = em.find(Customer.class, customer.getId());
            assertThat(reloaded.getName()).isEqualTo(name);
            assertThat(reloaded.getEmail().value()).isEqualTo(normalizedEmail);
            assertThat(reloaded.getEmail().value().codePointCount(0, reloaded.getEmail().value().length())).isEqualTo(254);
            return reloaded.getId();
        });
        assertThat(jdbc.queryForObject("select char_length(name) from customers where id=?", Integer.class, id)).isEqualTo(200);
        assertThat(jdbc.queryForObject("select char_length(email) from customers where id=?", Integer.class, id)).isEqualTo(254);
        assertThatThrownBy(() -> jdbc.update("insert into customers (id, name, email, active, version) values (?, 'invalid', ?, true, 0)", UUID.randomUUID(),
                "a".repeat(243) + "@example.com")).hasStackTraceContaining("ck_customers_email");
        assertThat(repository.findById(id).orElseThrow().getEmail().value()).isEqualTo(normalizedEmail);
    }
}
