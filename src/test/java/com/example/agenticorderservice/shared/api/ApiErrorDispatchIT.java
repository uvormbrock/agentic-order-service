package com.example.agenticorderservice.shared.api;

import static com.example.agenticorderservice.shared.api.ApiError.Code.*;
import static org.assertj.core.api.Assertions.*;

import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;

@org.springframework.boot.test.context.SpringBootTest(
        webEnvironment = org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.jpa.properties.hibernate.generate_statistics=true",
            "spring.datasource.hikari.connection-timeout=2500"
        })
@org.springframework.test.annotation.DirtiesContext(
        classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
class ApiErrorDispatchIT extends PostgreSqlApiTestSupport {
    @Test
    @org.junit.jupiter.api.Order(1)
    void actualServletTransportNeverFallsBackToHtmlForIncompatibleAccept() throws Exception {
        for (String path : new String[] {"/products", "/unmapped-path"}) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                    .header("Accept", "text/html")
                    .GET()
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertError(response, path.equals("/products") ? NOT_ACCEPTABLE : ENDPOINT_NOT_FOUND);
            assertThat(response.headers().firstValue("Content-Type").orElseThrow())
                    .startsWith("application/json");
        }
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void servletErrorDispatchUsesSafeWriter() throws Exception {
        // Calling /error without an exception exposes no framework attributes or diagnostics.
        var response = request("GET", "/error", null);
        assertError(response, INTERNAL_SERVER_ERROR);
        assertThat(response.body()).doesNotContain("trace", "timestamp", "path");
    }

    @org.springframework.beans.factory.annotation.Autowired
    org.testcontainers.postgresql.PostgreSQLContainer postgres;

    @org.springframework.beans.factory.annotation.Autowired
    javax.sql.DataSource contextDataSource;

    @Test
    @org.junit.jupiter.api.Order(3)
    void stoppedDisposableDatabaseIsConfirmedPreWriteUnavailability() throws Exception {
        // Only this class's project-managed disposable container is stopped; Compose is never accessed.
        postgres.stop();
        // Evict stale pooled handles so failure is proven at acquisition, before any write/commit.
        // A failure first observed during flush/commit must remain conservative 500 instead.
        contextDataSource
                .unwrap(com.zaxxer.hikari.HikariDataSource.class)
                .getHikariPoolMXBean()
                .softEvictConnections();
        assertError(request("POST", "/products", "{\"name\":\"Cannot Persist\",\"price\":1}"), SERVICE_UNAVAILABLE);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    @org.junit.jupiter.api.Order(2)
    void actualUntypedFixedLengthAndChunkedBodiesAreRejectedWithoutMutation(boolean chunked) throws Exception {
        long before = jdbc.queryForObject("select count(*) from products", Long.class);
        byte[] body = "{\"name\":\"Must not persist\",\"price\":1}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var publisher = chunked
                ? HttpRequest.BodyPublishers.ofInputStream(() -> new java.io.ByteArrayInputStream(body))
                : HttpRequest.BodyPublishers.ofByteArray(body);
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/products"))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(java.time.Duration.ofSeconds(20))
                .POST(publisher)
                .build(); // Deliberately no Content-Type; real nonempty bytes.
        assertError(client.send(request, HttpResponse.BodyHandlers.ofString()), UNSUPPORTED_MEDIA_TYPE);
        assertThat(jdbc.queryForObject("select count(*) from products", Long.class))
                .isEqualTo(before);
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void actualAbsentBodyAndInvalidUtf8RetainTheirDistinctSafeErrors() throws Exception {
        long before = jdbc.queryForObject("select count(*) from products", Long.class);
        var absent = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/products"))
                .timeout(java.time.Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        assertError(client.send(absent, HttpResponse.BodyHandlers.ofString()), REQUIRED_FIELD_MISSING);
        var invalid = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/products"))
                .timeout(java.time.Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(new byte[] {(byte) 0xff}))
                .build();
        assertError(client.send(invalid, HttpResponse.BodyHandlers.ofString()), MALFORMED_JSON);
        assertThat(jdbc.queryForObject("select count(*) from products", Long.class))
                .isEqualTo(before);
    }
}
