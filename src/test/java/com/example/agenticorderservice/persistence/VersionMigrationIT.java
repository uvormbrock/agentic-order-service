package com.example.agenticorderservice.persistence;

import static org.assertj.core.api.Assertions.*;

import com.example.agenticorderservice.PostgreSqlTestConfiguration;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("integration-test")
@Import(PostgreSqlTestConfiguration.class)
class VersionMigrationIT {
    @Autowired
    DataSource source;

    @Autowired
    Flyway current;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void upgradesRepresentativeV1DataWithoutChangingHistoryConstraintsOrDefaults() {
        String schema = "upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try {
            var v1 = Flyway.configure()
                    .dataSource(source)
                    .schemas(schema)
                    .defaultSchema(schema)
                    .target("1")
                    .load();
            assertThat(v1.migrate().migrationsExecuted).isEqualTo(1);
            Integer checksum = jdbc.queryForObject(
                    "select checksum from " + schema + ".flyway_schema_history where version='1'", Integer.class);
            UUID product = UUID.randomUUID(), customer = UUID.randomUUID(), order = UUID.randomUUID();
            jdbc.update("insert into " + schema + ".products values (?, 'Original', 12.34, false)", product);
            jdbc.update(
                    "insert into " + schema + ".customers values (?, 'Original', ?, false)",
                    customer,
                    customer + "@example.com");
            jdbc.update("insert into " + schema + ".orders values (?, ?, 12.34, 0, 12.34)", order, customer);
            jdbc.update(
                    "insert into " + schema + ".order_items values (?, ?, ?, 'Snapshot', 1, 12.34, 12.34)",
                    UUID.randomUUID(),
                    order,
                    product);
            UUID activeProduct = UUID.randomUUID(), activeCustomer = UUID.randomUUID();
            jdbc.update("insert into " + schema + ".products values (?, 'Active', 999999.99, true)", activeProduct);
            jdbc.update(
                    "insert into " + schema + ".customers values (?, 'Active', ?, true)",
                    activeCustomer,
                    activeCustomer + "@example.com");
            var indexes = jdbc.queryForList(
                    "select indexdef from pg_indexes where schemaname=? order by indexname", String.class, schema);
            int constraints = jdbc.queryForObject(
                    "select count(*) from pg_constraint c join pg_namespace n on n.oid=c.connamespace where n.nspname=?",
                    Integer.class,
                    schema);
            var v2 = Flyway.configure()
                    .dataSource(source)
                    .schemas(schema)
                    .defaultSchema(schema)
                    .load();
            assertThat(v2.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(jdbc.queryForList(
                            "select version from " + schema
                                    + ".flyway_schema_history where type='SQL' order by installed_rank",
                            String.class))
                    .containsExactly("1", "2");
            assertThat(java.util.Arrays.stream(v2.info().applied())
                            .filter(info -> info.getType().name().equals("SCHEMA"))
                            .count())
                    .isEqualTo(1);
            assertThat(v2.validateWithResult().validationSuccessful).isTrue();
            assertThat(jdbc.queryForObject(
                            "select checksum from " + schema + ".flyway_schema_history where version='1'",
                            Integer.class))
                    .isEqualTo(checksum);
            assertThat(jdbc.queryForObject(
                            "select count(*) from pg_constraint c join pg_namespace n on n.oid=c.connamespace where n.nspname=?",
                            Integer.class,
                            schema))
                    .isEqualTo(constraints);
            for (String table : new String[] {"products", "customers"}) {
                assertThat(jdbc.queryForObject(
                                "select count(*) from " + schema + "." + table + " where version=0", Long.class))
                        .isEqualTo(2);
                UUID original = table.equals("products") ? product : customer;
                UUID active = table.equals("products") ? activeProduct : activeCustomer;
                assertThat(jdbc.queryForObject(
                                "select active from " + schema + "." + table + " where id=?", Boolean.class, active))
                        .isTrue();
                var column = jdbc.queryForMap(
                        "select data_type,is_nullable,column_default from information_schema.columns where table_schema=? and table_name=? and column_name='version'",
                        schema,
                        table);
                assertThat(column).containsEntry("data_type", "bigint").containsEntry("is_nullable", "NO");
                assertThat(column.get("column_default")).isNull();
                assertThat(jdbc.queryForObject(
                                "select name from " + schema + "." + table + " where id=?", String.class, original))
                        .isEqualTo("Original");
                assertThat(jdbc.queryForObject(
                                "select active from " + schema + "." + table + " where id=?", Boolean.class, original))
                        .isFalse();
            }
            assertThat(jdbc.queryForList(
                            "select indexdef from pg_indexes where schemaname=? order by indexname",
                            String.class,
                            schema))
                    .isEqualTo(indexes);
            assertThat(jdbc.queryForObject("select product_name from " + schema + ".order_items", String.class))
                    .isEqualTo("Snapshot");
            jdbc.update("update " + schema + ".products set version=9 where id=?", product);
            assertThat(v2.migrate().migrationsExecuted).isZero();
            assertThat(v2.validateWithResult().validationSuccessful).isTrue();
            assertThat(jdbc.queryForObject(
                            "select version from " + schema + ".products where id=?", Long.class, product))
                    .isEqualTo(9);
            assertThat(current.info().applied()).hasSize(2); // Fresh context already validated V1+V2 mappings.
        } finally {
            // Only this test-generated schema in the disposable @ServiceConnection database.
            jdbc.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            assertThat(jdbc.queryForObject("select count(*) from pg_namespace where nspname=?", Long.class, schema))
                    .isZero();
        }
    }
}
