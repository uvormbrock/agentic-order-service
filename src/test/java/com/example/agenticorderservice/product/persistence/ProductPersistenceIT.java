package com.example.agenticorderservice.product.persistence;

import static com.example.agenticorderservice.PostgreSqlTestConfiguration.inTransaction;
import static org.assertj.core.api.Assertions.*;

import com.example.agenticorderservice.PostgreSqlTestConfiguration;
import com.example.agenticorderservice.product.domain.Product;
import com.example.agenticorderservice.shared.domain.Money;
import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("integration-test")
@Import(PostgreSqlTestConfiguration.class)
// Accepted D1/D3/D6/D7: exact boundary-price and Unicode round trips.
class ProductPersistenceIT {
    @Autowired
    EntityManagerFactory factory;

    @Autowired
    ProductRepository repository;

    @Test
    void roundTripsZeroAndMaximumPricesWithControlledUpdates() {
        for (String price : new String[] {"0.00", "999999.99"}) {
            var original = new Product("😀".repeat(200), Money.of(price));
            UUID id = inTransaction(factory, em -> {
                em.persist(original);
                em.flush();
                em.clear();
                var loaded = em.find(Product.class, original.getId());
                assertThat(loaded.getPrice()).isEqualTo(Money.of(price));
                assertThat(loaded.getName()).isEqualTo("😀".repeat(200));
                loaded.deactivate();
                loaded.update("Ändern", Money.of("25.05"));
                return loaded.getId();
            });
            var loaded = repository.findById(id).orElseThrow();
            assertThat(loaded.getPrice()).isEqualTo(Money.of("25.05"));
            assertThat(loaded.isActive()).isFalse();
        }
    }

    // Finding 4: accepted D1/D3, BR-001/BR-011/BR-012; updates do not replace UUID identity.
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void retainsGeneratedIdentityAndBothFieldsThroughApprovedAndRejectedUpdates(boolean inactive) {
        var original = new Product("Original", Money.of("25.00"));
        assertThat(original.getId()).isNull();
        UUID id = inTransaction(factory, em -> {
            em.persist(original);
            em.flush();
            UUID assignedId = original.getId();
            assertThat(assignedId).isNotNull();
            em.clear();
            var loaded = em.find(Product.class, assignedId);
            if (inactive) {
                loaded.deactivate();
            }
            loaded.update("\u2003 Äpfel😀 ", Money.of("40.05"));
            em.flush();
            em.clear();
            var reloaded = em.find(Product.class, assignedId);
            assertThat(reloaded.getId()).isEqualTo(assignedId);
            assertThat(reloaded.getName()).isEqualTo("Äpfel😀");
            assertThat(reloaded.getPrice()).isEqualTo(Money.of("40.05"));
            assertThat(reloaded.isActive()).isEqualTo(!inactive);
            assertThatNullPointerException().isThrownBy(() -> reloaded.update("Unwanted name", null));
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> reloaded.update("Unwanted name", Money.of("1000000.00")));
            assertThat(reloaded.getName()).isEqualTo("Äpfel😀");
            assertThat(reloaded.getPrice()).isEqualTo(Money.of("40.05"));
            assertThat(reloaded.isActive()).isEqualTo(!inactive);
            assertThat(reloaded.getId()).isEqualTo(assignedId);
            em.flush();
            em.clear();
            var afterRejectedUpdates = em.find(Product.class, assignedId);
            assertThat(afterRejectedUpdates.getId()).isEqualTo(assignedId);
            assertThat(afterRejectedUpdates.getName()).isEqualTo("Äpfel😀");
            assertThat(afterRejectedUpdates.getPrice()).isEqualTo(Money.of("40.05"));
            assertThat(afterRejectedUpdates.isActive()).isEqualTo(!inactive);
            return assignedId;
        });
        var committed = repository.findById(id).orElseThrow();
        assertThat(committed.getId()).isEqualTo(id);
        assertThat(committed.getName()).isEqualTo("Äpfel😀");
        assertThat(committed.getPrice()).isEqualTo(Money.of("40.05"));
        assertThat(committed.isActive()).isEqualTo(!inactive);
    }
}
