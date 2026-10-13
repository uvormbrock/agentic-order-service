package com.example.agenticorderservice.product.persistence;

import com.example.agenticorderservice.product.domain.Product;
import com.example.agenticorderservice.shared.domain.Money;
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
// Accepted D1/D3/D6/D7: exact boundary-price and Unicode round trips.
class ProductPersistenceIT {
    @Autowired EntityManagerFactory factory;
    @Autowired ProductRepository repository;

    @Test void roundTripsZeroAndMaximumPricesWithControlledUpdates() {
        for (String price : new String[]{"0.00", "999999.99"}) {
            var original = new Product("😀".repeat(200), Money.of(price));
            UUID id = inTransaction(factory, em -> {
                em.persist(original); em.flush(); em.clear();
                var loaded = em.find(Product.class, original.getId());
                assertThat(loaded.getPrice()).isEqualTo(Money.of(price));
                assertThat(loaded.getName()).isEqualTo("😀".repeat(200));
                loaded.deactivate(); loaded.update("Ändern", Money.of("25.05")); return loaded.getId();
            });
            var loaded = repository.findById(id).orElseThrow();
            assertThat(loaded.getPrice()).isEqualTo(Money.of("25.05")); assertThat(loaded.isActive()).isFalse();
        }
    }
}
