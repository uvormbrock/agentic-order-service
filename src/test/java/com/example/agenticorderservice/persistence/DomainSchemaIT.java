package com.example.agenticorderservice.persistence;

import org.flywaydb.core.Flyway;
import java.util.UUID;
import com.example.agenticorderservice.PostgreSqlTestConfiguration;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("integration-test")
@Import(PostgreSqlTestConfiguration.class)
// Accepted D6/D8/D10: actual PostgreSQL schema, negative constraints and migration history.
class DomainSchemaIT {
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired EntityManagerFactory factory;

    @Test void usesUnconstrainedNumericsAndRepeatableValidatedMigrationHistory() {
        assertThat(factory.getMetamodel().getEntities()).hasSize(4);
        assertThat(jdbc.queryForObject("select count(*) from information_schema.columns where table_schema='public' and data_type='numeric' and numeric_precision is null and numeric_scale is null", Long.class)).isEqualTo(6);
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        assertThat(flyway.info().applied()).hasSize(1);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("select checksum from flyway_schema_history where version='1'", Integer.class)).isNotNull();
    }
    @Test void rejectsInvalidPricesAtDatabaseBoundaryWithoutCoercion() {
        for (String invalid : new String[]{"-0.01", "1000000", "1.001", "10.000", "NaN", "Infinity", "-Infinity"}) {
            UUID id = UUID.randomUUID();
            assertThatThrownBy(() -> jdbc.update("insert into products values (?, 'invalid', cast(? as numeric), true)", id, invalid))
                    .hasStackTraceContaining("ck_products_price");
            assertThat(jdbc.queryForObject("select count(*) from products where id=?", Long.class, id)).isZero();
        }
    }
    @Test void enforcesNameUnicodeWhitespaceLengthAndRequiredFields() {
        for (String invalid : new String[]{"", " ", " padded ", "\tTabbed", "😀".repeat(201)}) {
            assertThatThrownBy(() -> jdbc.update("insert into products values (?, ?, 1.00, true)", UUID.randomUUID(), invalid))
                    .hasStackTraceContaining("ck_products_name");
        }
        assertThatThrownBy(() -> jdbc.update("insert into products values (?, null, 1.00, true)", UUID.randomUUID()))
                .hasStackTraceContaining("name");
    }
    @Test void enforcesCaseIndexAndNormalizedEmailAgainstDirectWrites() {
        UUID id = UUID.randomUUID(); String email = id + "@example.com";
        jdbc.update("insert into customers values (?, 'existing', ?, false)", id, email);
        assertThatThrownBy(() -> jdbc.update("insert into customers values (?, 'duplicate', ?, true)", UUID.randomUUID(), email))
                .hasStackTraceContaining("uq_customers_email");
        assertThatThrownBy(() -> jdbc.update("insert into customers values (?, 'invalid', ?, true)", UUID.randomUUID(), email.toUpperCase(java.util.Locale.ROOT)))
                .hasStackTraceContaining("ck_customers_email");
        assertThat(jdbc.queryForObject("select indexdef from pg_indexes where indexname='uq_customers_email'", String.class))
                .contains("UNIQUE").contains("lower(email)").doesNotContain(" WHERE ");
    }

    @Test void rejectsDanglingCustomerAndInvalidOrderAmounts() {
        assertThatThrownBy(() -> jdbc.update("insert into orders values (?, ?, 1.00, 0.00, 1.00)", UUID.randomUUID(), UUID.randomUUID()))
                .hasStackTraceContaining("fk_orders_customer");
        UUID customer = UUID.randomUUID();
        jdbc.update("insert into customers values (?, 'valid', ?, false)", customer, customer + "@example.com");
        for (String column : new String[]{"subtotal", "discount", "total"}) {
            for (String invalid : new String[]{"-0.01", "1.001", "10.000", "NaN", "Infinity", "-Infinity"}) {
                String values = switch (column) {
                    case "subtotal" -> "cast(? as numeric), 0.00, 1.00";
                    case "discount" -> "1.00, cast(? as numeric), 1.00";
                    default -> "1.00, 0.00, cast(? as numeric)";
                };
                assertThatThrownBy(() -> jdbc.update("insert into orders values (?, ?, " + values + ")", UUID.randomUUID(), customer, invalid))
                        .hasStackTraceContaining("ck_orders_" + column);
            }
        }
    }
}
