package com.example.agenticorderservice.shared.api;

public record ApiError(String code, String message) {
    public ApiError(Code value) {
        this(value.name(), value.message());
    }

    public enum Code {
        INVALID_ORDER_ITEMS(400, "The order must contain at least one item."),
        INVALID_ORDER_QUANTITY(
                400,
                "Order item quantities must be positive integers and the consolidated quantity must not exceed 10,000."),
        CUSTOMER_HAS_ORDERS(
                409,
                "Customer cannot be deleted because existing orders reference this customer. Deactivate the customer instead."),
        CUSTOMER_INACTIVE(409, "The customer is inactive."),
        PRODUCT_INACTIVE(409, "The product is inactive."),
        ORDER_NOT_FOUND(404, "The order was not found."),
        MALFORMED_JSON(400, "The request body must be a valid JSON object."),
        UNKNOWN_FIELD(400, "The request contains an unknown field."),
        UNSUPPORTED_FIELD(400, "The request contains a field or activation value that this operation does not permit."),
        INVALID_FIELD_TYPE(400, "A request field has an invalid JSON type."),
        REQUIRED_FIELD_MISSING(400, "A required request field is missing or null."),
        INVALID_NAME(400, "The name must contain 1 to 200 Unicode code points after stripping surrounding whitespace."),
        INVALID_EMAIL(400, "The email must satisfy the documented email policy and normalization limit."),
        INVALID_PRODUCT_PRICE(
                400,
                "The product price must be between EUR 0.00 and EUR 999,999.99, use ordinary decimal notation and have at most two decimal places."),
        INVALID_RESOURCE_ID(400, "The resource identifier must use canonical UUID text representation."),
        PRODUCT_NOT_FOUND(404, "The product was not found."),
        CUSTOMER_NOT_FOUND(404, "The customer was not found."),
        ENDPOINT_NOT_FOUND(404, "The requested endpoint was not found."),
        METHOD_NOT_ALLOWED(405, "This HTTP method is not supported for this resource."),
        OPERATION_NOT_AVAILABLE(405, "Customer deletion is not available in this API version."),
        NOT_ACCEPTABLE(406, "The requested response representation is not supported."),
        DUPLICATE_EMAIL(409, "The email address already belongs to another customer."),
        CONCURRENT_MODIFICATION(409, "The resource was modified concurrently. Please reload and retry."),
        UNSUPPORTED_MEDIA_TYPE(415, "The request media type is not supported."),
        INTERNAL_SERVER_ERROR(500, "An unexpected server error occurred."),
        SERVICE_UNAVAILABLE(503, "A required service is temporarily unavailable.");
        private final int status;
        private final String message;

        Code(int status, String message) {
            this.status = status;
            this.message = message;
        }

        public int status() {
            return status;
        }

        public String message() {
            return message;
        }
    }
}
