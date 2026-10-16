package com.example.agenticorderservice.product.api;

import static com.example.agenticorderservice.shared.api.ApiError.Code.*;
import static org.assertj.core.api.Assertions.*;

import com.example.agenticorderservice.shared.api.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class ProductApiIT extends PostgreSqlApiTestSupport {
    @Test
    void executesAllFiveEndpointsAgainstCommittedPostgreSqlState() throws Exception {
        Object value = new BigDecimal("12.34");
        var created =
                request("POST", "/products", mapper.writeValueAsString(Map.of("name", "  Älice😀  ", "price", value)));
        assertThat(created.statusCode()).isEqualTo(201);
        var id = json(created).get("id").asString();
        assertThat(created.headers().firstValue("Location")).contains("/products/" + id);
        assertProduct(json(created), UUID.fromString(id), "Älice😀", "12.34", true);
        assertThat(json(created).size()).isEqualTo(4);
        assertThat(json(created).has("version")).isFalse();
        assertThat(jdbc.queryForObject("select version from products where id=?", Long.class, UUID.fromString(id)))
                .isZero();
        var firstPatch = request("PATCH", "/products/" + id, "{\"active\":false}");
        assertThat(firstPatch.statusCode()).isEqualTo(200);
        Long version = jdbc.queryForObject("select version from products where id=?", Long.class, UUID.fromString(id));
        var stats = factory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.clear();
        var repeat = request("PATCH", "/products/" + id, "{\"active\":false}");
        assertThat(repeat.statusCode()).isEqualTo(200);
        assertThat(repeat.body()).isEqualTo(firstPatch.body());
        assertThat(stats.getEntityUpdateCount()).isZero();
        assertThat(jdbc.queryForObject("select version from products where id=?", Long.class, UUID.fromString(id)))
                .isEqualTo(version);
        Object changed = new BigDecimal("45.67");
        var updated = request(
                "PUT", "/products/" + id, mapper.writeValueAsString(Map.of("name", "Changed", "price", changed)));
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(json(updated).get("active").asBoolean()).isFalse();
        assertProduct(json(updated), UUID.fromString(id), "Changed", "45.67", false);
        assertThat(jdbc.queryForObject("select price from products where id=?", BigDecimal.class, UUID.fromString(id)))
                .isEqualByComparingTo("45.67");
        assertThat(jdbc.queryForObject("select id from products where id=?", UUID.class, UUID.fromString(id)))
                .isEqualTo(UUID.fromString(id));
        assertThat(jdbc.queryForObject("select active from products where id=?", Boolean.class, UUID.fromString(id)))
                .isFalse();
        assertThat(jdbc.queryForObject("select name from products where id=?", String.class, UUID.fromString(id)))
                .isEqualTo("Changed");
        assertThat(jdbc.queryForObject("select version from products where id=?", Long.class, UUID.fromString(id)))
                .isEqualTo(version + 1);
        stats.clear();
        var found = request("GET", "/products/" + id, null);
        assertThat(found.statusCode()).isEqualTo(200);
        assertThat(found.body()).isEqualTo(updated.body());
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1); // Full materialization: serialization adds no query.
        assertProduct(json(found), UUID.fromString(id), "Changed", "45.67", false);
        var listed = request("GET", "/products", null);
        assertThat(listed.statusCode()).isEqualTo(200);
        assertThat(json(listed)).anySatisfy(row -> {
            assertThat(row.size()).isEqualTo(4);
        });
        assertProduct(member(json(listed), UUID.fromString(id)), UUID.fromString(id), "Changed", "45.67", false);
        assertError(
                request("PUT", "/products/" + id, "{\"name\":\"Unwanted\",\"price\":null}"), REQUIRED_FIELD_MISSING);
        assertThat(request("GET", "/products/" + id, null).body()).isEqualTo(updated.body());
        assertError(request("GET", "/products/" + UUID.randomUUID(), null), PRODUCT_NOT_FOUND);
    }

    @Test
    void listsExactActiveAndInactiveFixturesRegardlessOfUnrelatedRows() throws Exception {
        UUID activeId = persistProduct("Active fixture", "17.89", true);
        UUID inactiveId = persistProduct("Inactive fixture", "67.89", false);
        persistProduct("Unrelated fixture", "99.99", true);
        assertThat(activeId).isNotEqualTo(inactiveId);
        var stats = factory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.clear();
        var response = request("GET", "/products", null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        assertProduct(member(json(response), activeId), activeId, "Active fixture", "17.89", true);
        assertProduct(member(json(response), inactiveId), inactiveId, "Inactive fixture", "67.89", false);
    }

    private UUID persistProduct(String name, String price, boolean active) {
        return com.example.agenticorderservice.PostgreSqlTestConfiguration.inTransaction(factory, em -> {
            var entity = new com.example.agenticorderservice.product.domain.Product(
                    name, com.example.agenticorderservice.shared.domain.Money.of(price));
            if (!active) {
                entity.deactivate();
            }
            em.persist(entity);
            return entity.getId();
        });
    }

    private tools.jackson.databind.JsonNode member(tools.jackson.databind.JsonNode array, UUID id) {
        var matches = new java.util.ArrayList<tools.jackson.databind.JsonNode>();
        array.forEach(row -> {
            if (id.toString().equals(row.get("id").asString())) {
                matches.add(row);
            }
        });
        assertThat(matches).as("Exactly one response row for fixture %s", id).hasSize(1);
        return matches.getFirst();
    }

    private void assertProduct(
            tools.jackson.databind.JsonNode row, UUID id, String name, String value, boolean active) {
        assertThat(row.propertyNames()).containsExactlyInAnyOrder("id", "name", "price", "active");
        assertThat(row.get("id").asString()).isEqualTo(id.toString());
        assertThat(row.get("name").asString()).isEqualTo(name);
        assertThat(row.get("price").decimalValue()).isEqualByComparingTo(value);
        assertThat(row.get("active").asBoolean()).isEqualTo(active);
    }
}
