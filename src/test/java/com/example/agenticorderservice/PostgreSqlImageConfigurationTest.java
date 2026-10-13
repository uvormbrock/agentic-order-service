package com.example.agenticorderservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostgreSqlImageConfigurationTest {

    @TempDir
    Path directory;

    @Test
    void reportsMissingConfigurationFile() {
        var missing = directory.resolve("missing.properties");
        assertThatThrownBy(() -> PostgreSqlTestConfiguration.loadPostgresImage(missing))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot read PostgreSQL image configuration")
                .hasMessageContaining("missing.properties")
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void reportsMissingImageProperty() throws IOException {
        var file = directory.resolve("postgres.properties");
        Files.writeString(file, "OTHER_SETTING=value\n");
        assertThatThrownBy(() -> PostgreSqlTestConfiguration.loadPostgresImage(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must define POSTGRES_IMAGE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "postgres", "postgres:latest", "other:17.11-bookworm", "${POSTGRES_IMAGE}"})
    void rejectsEmptyOrMalformedImageValues(String value) throws IOException {
        var file = directory.resolve("postgres.properties");
        Files.writeString(file, "POSTGRES_IMAGE=" + value + "\n");
        assertThatThrownBy(() -> PostgreSqlTestConfiguration.loadPostgresImage(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("POSTGRES_IMAGE")
                .hasMessageContaining("postgres:<major>.<patch>-bookworm");
    }

    @Test
    void reportsMalformedPropertiesSyntax() throws IOException {
        var file = directory.resolve("postgres.properties");
        Files.writeString(file, "POSTGRES_IMAGE=" + "\\" + "uZZZZ\n");
        assertThatThrownBy(() -> PostgreSqlTestConfiguration.loadPostgresImage(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot read PostgreSQL image configuration")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
