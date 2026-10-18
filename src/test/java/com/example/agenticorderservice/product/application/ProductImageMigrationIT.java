package com.example.agenticorderservice.product.application;

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
class ProductImageMigrationIT {
    @Autowired
    DataSource source;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void additiveImageMigrationPreservesActiveInactiveImagelessDataAndHistory() {
        String schema = "image_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try {
            var baseline = Flyway.configure()
                    .dataSource(source)
                    .schemas(schema)
                    .defaultSchema(schema)
                    .target("2")
                    .load();
            assertThat(baseline.migrate().migrationsExecuted).isEqualTo(2);
            UUID active = UUID.randomUUID(), inactive = UUID.randomUUID();
            jdbc.update(
                    "insert into " + schema + ".products values (?,'Active',25,true,7),(?,'Inactive',0,false,3)",
                    active,
                    inactive);
            var before = jdbc.queryForList("select * from " + schema + ".products order by id");
            var history = jdbc.queryForList("select version,checksum from " + schema
                    + ".flyway_schema_history where type='SQL' order by installed_rank");
            var upgraded = Flyway.configure()
                    .dataSource(source)
                    .schemas(schema)
                    .defaultSchema(schema)
                    .load();
            assertThat(upgraded.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(upgraded.migrate().migrationsExecuted).isZero();
            assertThat(upgraded.validateWithResult().validationSuccessful).isTrue();
            assertThat(jdbc.queryForList("select * from " + schema + ".products order by id"))
                    .isEqualTo(before);
            assertThat(
                            jdbc.queryForList(
                                    "select version,checksum from " + schema
                                            + ".flyway_schema_history where type='SQL' and version in ('1','2') order by installed_rank"))
                    .isEqualTo(history);
            assertThat(jdbc.queryForObject("select count(*) from " + schema + ".product_images", Integer.class))
                    .isZero();
            assertThat(jdbc.queryForList(
                            "select data_type from information_schema.columns where table_schema=? and table_name='product_images'",
                            String.class,
                            schema))
                    .doesNotContain("bytea", "oid");
        } finally {
            jdbc.execute("drop schema if exists " + schema + " cascade");
        }
    }
}
