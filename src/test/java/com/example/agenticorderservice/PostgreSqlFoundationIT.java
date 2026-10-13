package com.example.agenticorderservice;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration-test")
@Import(PostgreSqlTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PostgreSqlFoundationIT {

    @Autowired
    private PostgreSQLContainer postgres;
    @Autowired
    private DockerImageName postgresImage;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private Flyway flyway;
    @Autowired
    private ApplicationContext context;
    @Autowired
    private Environment environment;

    @Test
    void connectsToIsolatedPostgreSqlWithExpectedServerVersion() throws SQLException {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(postgres.getDockerImageName()).isEqualTo(postgresImage.asCanonicalNameString());
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            assertThat(connection.getMetaData().getURL()).isEqualTo(postgres.getJdbcUrl());
            assertThat(connection.getCatalog()).isEqualTo("agentic_order_service_it");
            try (var result = statement.executeQuery("SELECT 1")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
            try (var result = statement.executeQuery("SHOW server_version")) {
                assertThat(result.next()).isTrue();
                var expectedVersion = postgresImage.getVersionPart().split("-", 2)[0];
                var actualVersion = result.getString(1).split(" ", 2)[0];
                assertThat(actualVersion).isEqualTo(expectedVersion);
            }
        }
    }

    @Test
    void initializesJpaWithSchemaValidationAndWithoutOpenEntityManagerInView() {
        assertThat(entityManagerFactory.isOpen()).isTrue();
        assertThat(entityManagerFactory.getMetamodel().getEntities()).isEmpty();
        assertThat(environment.getProperty("spring.jpa.open-in-view", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(entityManagerFactory.getProperties().get("hibernate.hbm2ddl.auto")).isEqualTo("validate");
        assertThat(environment.getProperty("spring.sql.init.mode")).isEqualTo("never");
        assertThat(context.getBeansOfType(OpenEntityManagerInViewInterceptor.class)).isEmpty();
        assertThat(context.getBeansOfType(OpenEntityManagerInViewFilter.class)).isEmpty();
    }

    @Test
    void initializesOnlyFlywayMetadataWithoutApplicationMigrations() throws SQLException {
        assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
        assertThat(flyway.info().all()).isEmpty();
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            var tables = new ArrayList<String>();
            try (var result = statement.executeQuery("""
                    SELECT schemaname || '.' || tablename
                    FROM pg_tables
                    WHERE schemaname NOT LIKE 'pg_%' AND schemaname <> 'information_schema'
                    ORDER BY schemaname, tablename
                    """)) {
                while (result.next()) {
                    tables.add(result.getString(1));
                }
            }
            assertThat(tables).containsExactly("public.flyway_schema_history");
            try (var result = statement.executeQuery("SELECT count(*) FROM flyway_schema_history")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getLong(1)).isZero();
            }
        }
    }
}
