package com.example.agenticorderservice.customer.persistence;

import com.example.agenticorderservice.customer.domain.Customer;
import com.example.agenticorderservice.customer.domain.EmailAddress;
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
}
