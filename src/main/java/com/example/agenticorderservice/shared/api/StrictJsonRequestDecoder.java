package com.example.agenticorderservice.shared.api;

import com.example.agenticorderservice.product.api.*;
import com.example.agenticorderservice.customer.api.*;
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
import static com.example.agenticorderservice.shared.api.ApiError.Code.*;

/** Parse the entire document before selecting deterministic field/category failures. */
@Component
public final class StrictJsonRequestDecoder {
    private final JsonFactory factory = JsonFactory.builder().build();
    private record Scalar(JsonToken token, String text) { }

    public boolean supports(Class<?> type) {
        return type == ProductDetailsRequest.class || type == CustomerDetailsRequest.class
                || type == DeactivateProductRequest.class || type == DeactivateCustomerRequest.class;
    }
    public Object decode(byte[] bytes, Class<?> type) {
        if (!supports(type)) { throw new IllegalArgumentException("Unsupported request DTO"); }
        if (bytes.length == 0) { throw new ApiInputException(REQUIRED_FIELD_MISSING); }
        Map<String, Object> fields = parse(bytes);
        boolean product = type == ProductDetailsRequest.class || type == DeactivateProductRequest.class;
        boolean patch = type == DeactivateProductRequest.class || type == DeactivateCustomerRequest.class;
        List<String> required = patch ? List.of("active") : product ? List.of("name", "price") : List.of("name", "email");
        Set<String> known = product ? Set.of("id", "version", "active", "name", "price") : Set.of("id", "version", "active", "name", "email");
        for (String key : new TreeSet<>(fields.keySet())) {
            if (!known.contains(key)) { throw new ApiInputException(UNKNOWN_FIELD); }
            if (!required.contains(key)) { throw new ApiInputException(UNSUPPORTED_FIELD); }
            if (patch && fields.get(key) instanceof Scalar value
                    && value.token() == JsonToken.VALUE_TRUE) { throw new ApiInputException(UNSUPPORTED_FIELD); }
        }
        for (String key : required) {
            Object value = fields.get(key);
            if (value == null) { continue; }
            if (!(value instanceof Scalar scalar) || !validType(key, scalar.token())) {
                throw new ApiInputException(INVALID_FIELD_TYPE);
            }
        }
        for (String key : required) {
            if (fields.get(key) == null) { throw new ApiInputException(REQUIRED_FIELD_MISSING); }
        }
        if (patch) { return product ? new DeactivateProductRequest(false) : new DeactivateCustomerRequest(false); }
        String name = InputValidationException.name(((Scalar) fields.get("name")).text());
        if (product) {
            String spelling = ((Scalar) fields.get("price")).text();
            if (!spelling.matches("-?(0|[1-9][0-9]*)(\\.[0-9]{1,2})?")) { throw new ApiInputException(INVALID_PRODUCT_PRICE); }
            BigDecimal price = new BigDecimal(spelling);
            com.example.agenticorderservice.product.application.ProductApplicationService.validatePrice(price);
            return new ProductDetailsRequest(name, price);
        }
        String email = com.example.agenticorderservice.customer.application.CustomerApplicationService.validateEmail(((Scalar) fields.get("email")).text()).value();
        return new CustomerDetailsRequest(name, email);
    }
    private boolean validType(String key, JsonToken token) {
        return switch (key) {
            case "price" -> token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT;
            case "active" -> token == JsonToken.VALUE_TRUE || token == JsonToken.VALUE_FALSE;
            default -> token == JsonToken.VALUE_STRING;
        };
    }
    private Map<String, Object> parse(byte[] bytes) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            try (JsonParser parser = factory.createParser(text)) {
                if (parser.nextToken() != JsonToken.START_OBJECT) { throw new ApiInputException(MALFORMED_JSON); }
                Map<String, Object> result = object(parser);
                if (parser.nextToken() != null) { throw new ApiInputException(MALFORMED_JSON); }
                return result;
            }
        } catch (JacksonException | CharacterCodingException failure) { throw new ApiInputException(MALFORMED_JSON); }
    }
    private Map<String, Object> object(JsonParser parser) {
        Map<String, Object> fields = new HashMap<>();
        Set<String> seen = new HashSet<>();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (parser.currentToken() != JsonToken.PROPERTY_NAME) { throw new ApiInputException(MALFORMED_JSON); }
            String key = parser.currentName();
            if (!seen.add(key)) { throw new ApiInputException(MALFORMED_JSON); }
            if (parser.nextToken() == null) { throw new ApiInputException(MALFORMED_JSON); }
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
                    if (parser.currentToken() == null) { throw new ApiInputException(MALFORMED_JSON); }
                    values.add(value(parser));
                }
                yield values;
            }
            case VALUE_NULL -> null;
            case VALUE_STRING, VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT, VALUE_TRUE, VALUE_FALSE -> new Scalar(parser.currentToken(), parser.getText());
            default -> throw new ApiInputException(MALFORMED_JSON);
        };
    }
}
