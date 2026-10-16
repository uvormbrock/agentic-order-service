package com.example.agenticorderservice.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

// Distinct context-cache key gives this class its own disposable PostgreSQL container.
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "test.fixture=isolated-empty-resource-lists",
            "spring.jpa.properties.hibernate.generate_statistics=true"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EmptyResourceListsIT extends PostgreSqlApiTestSupport {
    @ParameterizedTest
    @ValueSource(strings = {"products", "customers"})
    void returnsEmptyArrayFromItsOwnFreshDatabaseWithoutResettingSharedData(String feature) throws Exception {
        assertThat(jdbc.queryForObject("select count(*) from " + feature, Long.class))
                .isZero();
        var stats = factory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.clear();
        var response = request("GET", "/" + feature, null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/json");
        assertThat(response.body()).isEqualTo("[]");
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from " + feature, Long.class))
                .isZero();
    }
}
