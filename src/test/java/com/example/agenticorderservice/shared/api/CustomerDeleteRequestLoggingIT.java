package com.example.agenticorderservice.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import com.example.agenticorderservice.shared.logging.EventCapture;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerDeleteRequestLoggingIT extends PostgreSqlApiTestSupport {
    @Test
    void realCustomerResponsesCompleteExactlyOnceWithCorrectStatusAndCorrelation() throws Exception {
        UUID unreferenced = customer(), referenced = customer();
        var product = request("POST", "/products", "{\"name\":\"Fixture\",\"price\":10}");
        assertThat(product.statusCode()).isEqualTo(201);
        var created = request(
                "POST",
                "/orders",
                mapper.writeValueAsString(Map.of(
                        "customerId",
                        referenced,
                        "items",
                        java.util.List.of(
                                Map.of("productId", json(product).get("id").asString(), "quantity", 1)))));
        assertThat(created.statusCode()).isEqualTo(201);
        UUID order = UUID.fromString(json(created).get("id").asString());
        var customerBefore = jdbc.queryForMap("select * from customers where id=?", referenced);
        var orderBefore = jdbc.queryForMap("select * from orders where id=?", order);
        var itemsBefore = jdbc.queryForList("select * from order_items where order_id=?", order);
        String historical = request("GET", "/orders/" + order, null).body();

        try (var capture = new EventCapture("com.example.agenticorderservice")) {
            var listed = request("GET", "/customers", null);
            completion(capture, listed, "GET", "/customers", 200, Level.INFO);
            var removed = request("DELETE", "/customers/" + unreferenced, null);
            completion(capture, removed, "DELETE", "/customers/{id}", 204, Level.INFO);
            assertThat(removed.body()).isEmpty();
            var rejected = request("DELETE", "/customers/" + referenced, null);
            assertError(rejected, ApiError.Code.CUSTOMER_HAS_ORDERS);
            completion(capture, rejected, "DELETE", "/customers/{id}", 409, Level.DEBUG);
            String rejectedId = rejected.headers().firstValue("X-Request-ID").orElseThrow();
            assertThat(capture.events.stream()
                            .filter(entry ->
                                    rejectedId.equals(entry.getMDCPropertyMap().get("requestId"))))
                    .noneSatisfy(entry -> assertThat(entry.getFormattedMessage())
                            .containsAnyOf("CUSTOMER_DELETED", "HTTP_TECHNICAL_FAILURE"));
            assertThat(java.util.Set.of(
                            listed.headers().firstValue("X-Request-ID").orElseThrow(),
                            removed.headers().firstValue("X-Request-ID").orElseThrow(),
                            rejectedId))
                    .hasSize(3);
            assertThat(jdbc.queryForObject("select count(*) from customers where id=?", Integer.class, unreferenced))
                    .isZero();
            assertThat(jdbc.queryForMap("select * from customers where id=?", referenced))
                    .isEqualTo(customerBefore);
            assertThat(jdbc.queryForMap("select * from orders where id=?", order))
                    .isEqualTo(orderBefore);
            assertThat(jdbc.queryForList("select * from order_items where order_id=?", order))
                    .isEqualTo(itemsBefore);
            assertThat(request("GET", "/orders/" + order, null).body()).isEqualTo(historical);
        }
    }

    private UUID customer() throws Exception {
        var created = request(
                "POST",
                "/customers",
                mapper.writeValueAsString(Map.of("name", "Fixture", "email", UUID.randomUUID() + "@example.com")));
        assertThat(created.statusCode()).isEqualTo(201);
        return UUID.fromString(json(created).get("id").asString());
    }

    private void completion(
            EventCapture capture, HttpResponse<String> response, String method, String route, int status, Level level)
            throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        String id = response.headers().firstValue("X-Request-ID").orElseThrow();
        assertThat(UUID.fromString(id)).hasToString(id);
        var event = capture.await(id, "HTTP_REQUEST_COMPLETED");
        assertThat(event.getFormattedMessage())
                .startsWith("event=HTTP_REQUEST_COMPLETED method=" + method + " route=" + route + " status=" + status
                        + " outcome=COMPLETED ");
        assertThat(event.getLevel()).isEqualTo(level);
        assertThat(event.getMDCPropertyMap()).containsEntry("requestId", id);
        assertThat(capture.events.stream()
                        .filter(entry -> id.equals(entry.getMDCPropertyMap().get("requestId")))
                        .filter(entry -> entry.getFormattedMessage().startsWith("event=HTTP_REQUEST_COMPLETED ")))
                .hasSize(1);
    }
}
