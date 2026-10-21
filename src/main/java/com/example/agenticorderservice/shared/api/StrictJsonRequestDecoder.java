package com.example.agenticorderservice.shared.api;

import static com.example.agenticorderservice.shared.api.ApiError.Code.*;

import com.example.agenticorderservice.customer.api.*;
import com.example.agenticorderservice.product.api.*;
import com.example.agenticorderservice.shared.application.InputValidationException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Component;
import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;

/** Parse the entire document before selecting deterministic field/category failures. */
@Component
public final class StrictJsonRequestDecoder {
    private final JsonFactory factory = JsonFactory.builder().build();

    private record Scalar(JsonToken token, String text) {}

    public boolean supports(Class<?> type) {
        return type == com.example.agenticorderservice.order.api.CreateOrderRequest.class
                || type == ProductDetailsRequest.class
                || type == CustomerDetailsRequest.class
                || type == DeactivateProductRequest.class
                || type == DeactivateCustomerRequest.class;
    }

    private static final String CUSTOMER_ID = "customerId";
    private static final String PRODUCT_ID = "productId";
    private static final String QUANTITY = "quantity";
    private static final String VERSION = "version";
    private static final String ITEMS = "items";
    private static final String NAME = "name";
    private static final String PRICE = "price";
    private static final String EMAIL = "email";
    private static final String ACTIVE = "active";

    public Object decode(byte[] bytes, Class<?> type) {
        if (!supports(type)) {
            throw new IllegalArgumentException("Unsupported request DTO");
        }
        if (bytes.length == 0) {
            throw new ApiInputException(REQUIRED_FIELD_MISSING);
        }
        Map<String, Object> fields = parse(bytes);
        if (type == com.example.agenticorderservice.order.api.CreateOrderRequest.class) {
            return order(fields);
        }
        boolean product = type == ProductDetailsRequest.class || type == DeactivateProductRequest.class;
        boolean patch = type == DeactivateProductRequest.class || type == DeactivateCustomerRequest.class;
        List<String> required = requiredFields(product, patch);
        validateProperties(fields, required, product, patch);
        validateTypes(fields, required);
        validatePresence(fields, required);
        return decodeValidatedFields(fields, product, patch);
    }

    private Object order(Map<String, Object> fields) {
        validateOrderProperties(fields);
        List<?> items = typedOrderItems(fields);
        validateOrderPresence(fields, items);
        UUID customerId = orderId(fields.get(CUSTOMER_ID));
        return new com.example.agenticorderservice.order.api.CreateOrderRequest(customerId, orderLines(items));
    }

    private void validateOrderProperties(Map<String, Object> fields) {
        var propertyErrors = new TreeMap<String, ApiError.Code>();
        orderProperties(
                fields,
                Set.of(CUSTOMER_ID, ITEMS),
                Set.of("id", VERSION, "customer", "currency", "subtotal", "discount", "total", "unitPrice"),
                "",
                propertyErrors);
        Object rawItems = fields.get(ITEMS);
        if (rawItems instanceof List<?> items) {
            for (int index = 0; index < items.size(); index++) {
                if (items.get(index) instanceof Map<?, ?> item) {
                    orderProperties(
                            item,
                            Set.of(PRODUCT_ID, QUANTITY),
                            Set.of(
                                    "id",
                                    VERSION,
                                    "productName",
                                    "unitPrice",
                                    "lineTotal",
                                    "subtotal",
                                    "discount",
                                    "total",
                                    "currency"),
                            "items[" + index + "].",
                            propertyErrors);
                }
            }
        }
        if (!propertyErrors.isEmpty()) {
            throw new ApiInputException(propertyErrors.firstEntry().getValue());
        }
    }

    private List<?> typedOrderItems(Map<String, Object> fields) {
        Object rawItems = fields.get(ITEMS);
        scalarType(fields.get(CUSTOMER_ID), JsonToken.VALUE_STRING);
        if (rawItems != null && !(rawItems instanceof List<?>)) {
            throw new ApiInputException(INVALID_FIELD_TYPE);
        }
        List<?> items = rawItems instanceof List<?> list ? list : List.of();
        for (Object raw : items) {
            if (raw == null) {
                continue;
            }
            if (!(raw instanceof Map<?, ?> item)) {
                throw new ApiInputException(INVALID_FIELD_TYPE);
            }
            scalarType(item.get(PRODUCT_ID), JsonToken.VALUE_STRING);
            scalarType(item.get(QUANTITY), JsonToken.VALUE_NUMBER_INT);
        }
        return items;
    }

    private void validateOrderPresence(Map<String, Object> fields, List<?> items) {
        if (fields.get(CUSTOMER_ID) == null || fields.get(ITEMS) == null) {
            throw new ApiInputException(REQUIRED_FIELD_MISSING);
        }
        for (Object raw : items) {
            if (raw == null) {
                throw new ApiInputException(REQUIRED_FIELD_MISSING);
            }
            Map<?, ?> item = (Map<?, ?>) raw;
            if (item.get(PRODUCT_ID) == null || item.get(QUANTITY) == null) {
                throw new ApiInputException(REQUIRED_FIELD_MISSING);
            }
        }
    }

    private List<com.example.agenticorderservice.order.api.OrderItemRequest> orderLines(List<?> items) {
        if (items.isEmpty()) {
            throw new ApiInputException(INVALID_ORDER_ITEMS);
        }
        List<com.example.agenticorderservice.order.api.OrderItemRequest> result = new ArrayList<>();
        Map<UUID, Integer> sums = new HashMap<>();
        for (Object raw : items) {
            Map<?, ?> item = (Map<?, ?>) raw;
            UUID productId = orderId(item.get(PRODUCT_ID));
            var number = new java.math.BigInteger(((Scalar) item.get(QUANTITY)).text());
            if (number.signum() <= 0 || number.compareTo(java.math.BigInteger.valueOf(10000)) > 0) {
                throw new ApiInputException(INVALID_ORDER_QUANTITY);
            }
            int quantity = number.intValueExact();
            int total = sums.getOrDefault(productId, 0) + quantity;
            if (total > 10000) {
                throw new ApiInputException(INVALID_ORDER_QUANTITY);
            }
            sums.put(productId, total);
            result.add(new com.example.agenticorderservice.order.api.OrderItemRequest(productId, quantity));
        }
        return result;
    }

    private void orderProperties(
            Map<?, ?> fields,
            Set<String> allowed,
            Set<String> forbidden,
            String prefix,
            Map<String, ApiError.Code> errors) {
        for (Object key : fields.keySet()) {
            String name = (String) key;
            if (!allowed.contains(name)) {
                errors.put(prefix + name, forbidden.contains(name) ? UNSUPPORTED_FIELD : UNKNOWN_FIELD);
            }
        }
    }

    private void scalarType(Object value, JsonToken expected) {
        if (value != null && (!(value instanceof Scalar scalar) || scalar.token() != expected)) {
            throw new ApiInputException(INVALID_FIELD_TYPE);
        }
    }

    private UUID orderId(Object value) {
        String text = ((Scalar) value).text();
        if (!text.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw new ApiInputException(INVALID_RESOURCE_ID);
        }
        return UUID.fromString(text);
    }

    private List<String> requiredFields(boolean product, boolean patch) {
        if (patch) {
            return List.of(ACTIVE);
        }
        if (product) {
            return List.of(NAME, PRICE);
        }
        return List.of(NAME, EMAIL);
    }

    private void validateProperties(Map<String, Object> fields, List<String> required, boolean product, boolean patch) {
        Set<String> known =
                product ? Set.of("id", VERSION, ACTIVE, NAME, PRICE) : Set.of("id", VERSION, ACTIVE, NAME, EMAIL);
        for (String key : new TreeSet<>(fields.keySet())) {
            if (!known.contains(key)) {
                throw new ApiInputException(UNKNOWN_FIELD);
            }
            if (!required.contains(key)) {
                throw new ApiInputException(UNSUPPORTED_FIELD);
            }
            if (patch && fields.get(key) instanceof Scalar value && value.token() == JsonToken.VALUE_TRUE) {
                throw new ApiInputException(UNSUPPORTED_FIELD);
            }
        }
    }

    private void validateTypes(Map<String, Object> fields, List<String> required) {
        for (String key : required) {
            Object value = fields.get(key);
            if (value == null) {
                continue;
            }
            if (!(value instanceof Scalar scalar) || !validType(key, scalar.token())) {
                throw new ApiInputException(INVALID_FIELD_TYPE);
            }
        }
    }

    private void validatePresence(Map<String, Object> fields, List<String> required) {
        for (String key : required) {
            if (fields.get(key) == null) {
                throw new ApiInputException(REQUIRED_FIELD_MISSING);
            }
        }
    }

    private Object decodeValidatedFields(Map<String, Object> fields, boolean product, boolean patch) {
        if (patch) {
            return product ? new DeactivateProductRequest(false) : new DeactivateCustomerRequest(false);
        }
        String name = InputValidationException.name(((Scalar) fields.get(NAME)).text());
        if (product) {
            return new ProductDetailsRequest(name, decodePrice(((Scalar) fields.get(PRICE)).text()));
        }
        String email = com.example.agenticorderservice.customer.application.CustomerApplicationService.normalizeEmail(
                ((Scalar) fields.get(EMAIL)).text());
        return new CustomerDetailsRequest(name, email);
    }

    private BigDecimal decodePrice(String spelling) {
        // Java's default regex mode keeps \d ASCII-only; do not enable UNICODE_CHARACTER_CLASS.
        if (!spelling.matches("-?(0|[1-9]\\d*)(\\.\\d{1,2})?")) {
            throw new ApiInputException(INVALID_PRODUCT_PRICE);
        }
        BigDecimal price = new BigDecimal(spelling);
        com.example.agenticorderservice.product.application.ProductApplicationService.validatePrice(price);
        return price;
    }

    private boolean validType(String key, JsonToken token) {
        return switch (key) {
            case PRICE -> token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT;
            case ACTIVE -> token == JsonToken.VALUE_TRUE || token == JsonToken.VALUE_FALSE;
            default -> token == JsonToken.VALUE_STRING;
        };
    }

    private Map<String, Object> parse(byte[] bytes) {
        try {
            String text = StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            try (JsonParser parser = factory.createParser(text)) {
                if (parser.nextToken() != JsonToken.START_OBJECT) {
                    throw new ApiInputException(MALFORMED_JSON);
                }
                Map<String, Object> result = object(parser);
                if (parser.nextToken() != null) {
                    throw new ApiInputException(MALFORMED_JSON);
                }
                return result;
            }
        } catch (JacksonException | CharacterCodingException failure) {
            throw new ApiInputException(MALFORMED_JSON);
        }
    }

    private Map<String, Object> object(JsonParser parser) {
        Map<String, Object> fields = new HashMap<>();
        Set<String> seen = new HashSet<>();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (parser.currentToken() != JsonToken.PROPERTY_NAME) {
                throw new ApiInputException(MALFORMED_JSON);
            }
            String key = parser.currentName();
            if (!seen.add(key)) {
                throw new ApiInputException(MALFORMED_JSON);
            }
            if (parser.nextToken() == null) {
                throw new ApiInputException(MALFORMED_JSON);
            }
            fields.put(key, value(parser));
        }
        return fields;
    }

    private Object value(JsonParser parser) {
        return switch (parser.currentToken()) {
            case START_OBJECT -> object(parser);
            case START_ARRAY -> {
                List<Object> values = new ArrayList<>();
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    if (parser.currentToken() == null) {
                        throw new ApiInputException(MALFORMED_JSON);
                    }
                    values.add(value(parser));
                }
                yield values;
            }
            case VALUE_NULL -> null;
            case VALUE_STRING, VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT, VALUE_TRUE, VALUE_FALSE ->
                new Scalar(parser.currentToken(), parser.getText());
            default -> throw new ApiInputException(MALFORMED_JSON);
        };
    }
}
