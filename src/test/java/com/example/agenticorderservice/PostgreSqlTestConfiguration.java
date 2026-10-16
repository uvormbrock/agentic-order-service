package com.example.agenticorderservice;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class PostgreSqlTestConfiguration {

    @Bean
    DockerImageName postgresImage() {
        return loadPostgresImage(Path.of("config", "postgres.properties"));
    }

    static DockerImageName loadPostgresImage(Path path) {
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Cannot read PostgreSQL image configuration from " + path, exception);
        }
        var image = properties.getProperty("POSTGRES_IMAGE");
        if (image == null || !image.matches("postgres:[0-9]+\\.[0-9]+-bookworm")) {
            throw new IllegalStateException(path + " must define POSTGRES_IMAGE "
                    + "as a literal official postgres:<major>.<patch>-bookworm image");
        }
        return DockerImageName.parse(image);
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres(DockerImageName postgresImage) {
        return new PostgreSQLContainer(postgresImage)
                .withDatabaseName("agentic_order_service_it")
                .withUsername("integration_test")
                .withPassword("integration_test")
                .withReuse(false);
    }
    /** Test-harness transaction only; no production use-case coordination. */
    public static <T> T inTransaction(
            jakarta.persistence.EntityManagerFactory factory,
            java.util.function.Function<jakarta.persistence.EntityManager, T> action) {
        try (var manager = factory.createEntityManager()) {
            var transaction = manager.getTransaction();
            transaction.begin();
            try {
                T value = action.apply(manager);
                transaction.commit();
                return value;
            } catch (RuntimeException | Error failure) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw failure;
            }
        }
    }
}
