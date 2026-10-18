package com.example.agenticorderservice.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/** Literal test oracle transcribed from docs/02-architecture/api-error-codes.md. */
public final class ExpectedApiErrors {
    public record Expected(int status, String message) {}

    private static final JsonMapper JSON = JsonMapper.builder().build();
    public static final Map<String, Expected> CATALOG = Map.ofEntries(
            Map.entry("INVALID_ORDER_ITEMS", new Expected(400, "The order must contain at least one item.")),
            Map.entry(
                    "INVALID_ORDER_QUANTITY",
                    new Expected(
                            400,
                            "Order item quantities must be positive integers and the consolidated quantity must not exceed 10,000.")),
            Map.entry(
                    "CUSTOMER_HAS_ORDERS",
                    new Expected(
                            409,
                            "Customer cannot be deleted because existing orders reference this customer. Deactivate the customer instead.")),
            Map.entry("CUSTOMER_INACTIVE", new Expected(409, "The customer is inactive.")),
            Map.entry("PRODUCT_INACTIVE", new Expected(409, "The product is inactive.")),
            Map.entry("ORDER_NOT_FOUND", new Expected(404, "The order was not found.")),
            Map.entry("MALFORMED_JSON", new Expected(400, "The request body must be a valid JSON object.")),
            Map.entry("UNKNOWN_FIELD", new Expected(400, "The request contains an unknown field.")),
            Map.entry(
                    "UNSUPPORTED_FIELD",
                    new Expected(
                            400,
                            "The request contains a field or activation value that this operation does not permit.")),
            Map.entry("INVALID_FIELD_TYPE", new Expected(400, "A request field has an invalid JSON type.")),
            Map.entry("REQUIRED_FIELD_MISSING", new Expected(400, "A required request field is missing or null.")),
            Map.entry(
                    "INVALID_NAME",
                    new Expected(
                            400,
                            "The name must contain 1 to 200 Unicode code points after stripping surrounding whitespace.")),
            Map.entry(
                    "INVALID_EMAIL",
                    new Expected(400, "The email must satisfy the documented email policy and normalization limit.")),
            Map.entry(
                    "INVALID_PRODUCT_PRICE",
                    new Expected(
                            400,
                            "The product price must be between EUR 0.00 and EUR 999,999.99, use ordinary decimal notation and have at most two decimal places.")),
            Map.entry(
                    "INVALID_RESOURCE_ID",
                    new Expected(400, "The resource identifier must use canonical UUID text representation.")),
            Map.entry("PRODUCT_NOT_FOUND", new Expected(404, "The product was not found.")),
            Map.entry("CUSTOMER_NOT_FOUND", new Expected(404, "The customer was not found.")),
            Map.entry("ENDPOINT_NOT_FOUND", new Expected(404, "The requested endpoint was not found.")),
            Map.entry("METHOD_NOT_ALLOWED", new Expected(405, "This HTTP method is not supported for this resource.")),
            Map.entry(
                    "OPERATION_NOT_AVAILABLE",
                    new Expected(405, "Customer deletion is not available in this API version.")),
            Map.entry("NOT_ACCEPTABLE", new Expected(406, "The requested response representation is not supported.")),
            Map.entry("DUPLICATE_EMAIL", new Expected(409, "The email address already belongs to another customer.")),
            Map.entry(
                    "CONCURRENT_MODIFICATION",
                    new Expected(409, "The resource was modified concurrently. Please reload and retry.")),
            Map.entry("UNSUPPORTED_MEDIA_TYPE", new Expected(415, "The request media type is not supported.")),
            Map.entry("INTERNAL_SERVER_ERROR", new Expected(500, "An unexpected server error occurred.")),
            Map.entry("SERVICE_UNAVAILABLE", new Expected(503, "A required service is temporarily unavailable.")));

    private ExpectedApiErrors() {}

    public static void assertResponse(int status, String body, String code) {
        Expected expected = CATALOG.get(code);
        assertThat(expected).as("Approved error code %s", code).isNotNull();
        assertThat(status).isEqualTo(expected.status());
        var json = JSON.readTree(body);
        assertThat(json.propertyNames()).containsExactlyInAnyOrder("code", "message");
        assertThat(json.get("code").asString()).isEqualTo(code);
        assertThat(json.get("message").asString()).isEqualTo(expected.message());
    }
}
