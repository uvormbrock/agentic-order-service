package com.example.agenticorderservice.configuration;

import static com.example.agenticorderservice.shared.api.ApiError.Code.*;

import com.example.agenticorderservice.shared.api.ApiError.Code;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.*;
import io.swagger.v3.oas.models.responses.*;
import io.swagger.v3.oas.models.servers.Server;
import java.util.*;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.*;

/** Documentation metadata only: request validation remains owned by the existing decoder and domain. */
@Configuration(proxyBeanMethods = false)
@Profile("demo")
public class OpenApiConfiguration {
    private static final String MESSAGE = "message";
    private static final String ACTIVE = "active";
    private static final String CUSTOMERS = "customers";
    private static final String JSON = "application/json";
    private static final String REQUEST_ID = "X-Request-ID";
    private static final String UUID_EXAMPLE = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    @Bean
    OpenAPI orderServiceApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Agentic Order Service")
                        .version("0.0.1-SNAPSHOT")
                        .description("""
                        Interactive documentation for the isolated local demo. Write requests change real demo data.
                        JSON writes require application/json (UTF-8). Unknown/forbidden properties, duplicate keys,
                        trailing JSON, scalar coercion and caller-supplied calculated amounts are rejected.
                        Validation precedence: routing/method/headers, UUID paths, syntax, properties, types,
                        required fields, business values, then resource lookup/conflicts. UUID text must be canonical;
                        hexadecimal case is accepted. Every normal API response has a server-generated X-Request-ID;
                        inbound IDs are ignored. Transport failure can prevent delivery. Unsupported methods use 405
                        with Allow; unknown routes use ENDPOINT_NOT_FOUND/404. These are routing errors, not extra operations.
                        Error responses contain exactly code/message. A 500 does not assert rollback or authorize replay;
                        uncertain commits are never replayed. No authentication, ETags or idempotency-key contract is implied.
                        """))
                .servers(List.of(new Server().url("/").description("Same origin as the running demo")))
                .components(new Components()
                        .addHeaders(
                                "RequestId",
                                new Header()
                                        .description("Server-generated UUID; inbound IDs are ignored")
                                        .schema(new StringSchema()
                                                .format("uuid")
                                                .example(UUID_EXAMPLE))));
    }

    @Bean
    OpenApiCustomizer actualApiContracts() {
        return api -> {
            // springdoc prunes unreferenced schemas before customizers run; register beside the response references.
            api.getComponents()
                    .addSchemas(
                            "ApiError",
                            new ObjectSchema()
                                    .addProperty("code", new StringSchema())
                                    .addProperty(MESSAGE, new StringSchema())
                                    .required(List.of("code", MESSAGE))
                                    .additionalProperties(false));
            api.getComponents().getSchemas().forEach(OpenApiConfiguration::customizeSchema);
            api.getPaths()
                    .forEach((path, item) -> item.readOperationsMap()
                            .forEach((method, operation) -> customizeOperation(path, method, operation)));
        };
    }

    private static void customizeSchema(String name, Schema<?> schema) {
        if (schema.getProperties() == null) return;
        schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
        if (name.endsWith("Request")) schema.setAdditionalProperties(false);
        if (name.startsWith("Deactivate")) {
            BooleanSchema active = new BooleanSchema();
            active.setEnum(List.of(false));
            active.description("Only false is accepted; reactivation is unsupported");
            schema.getProperties().put(ACTIVE, active);
        }
    }

    private static void customizeOperation(String path, PathItem.HttpMethod method, Operation operation) {
        String feature = path.split("/")[1];
        addErrorResponses(operation.getResponses(), errors(path, feature, method));
        customizeSuccessResponse(path, feature, method, operation.getResponses());
        operation
                .getResponses()
                .values()
                .forEach(value ->
                        value.addHeaderObject(REQUEST_ID, new Header().$ref("#/components/headers/RequestId")));
        customizeRequestBody(operation.getRequestBody());
        customizePathParameters(operation.getParameters());
    }

    private static void addErrorResponses(ApiResponses responses, List<Code> codes) {
        Map<Integer, List<Code>> byStatus = new LinkedHashMap<>();
        codes.forEach(code -> byStatus.computeIfAbsent(code.status(), ignored -> new ArrayList<>())
                .add(code));
        byStatus.forEach((status, values) -> {
            var media = new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiError"));
            values.forEach(code -> media.addExamples(
                    code.name(),
                    new Example().summary(code.name()).value(Map.of("code", code.name(), MESSAGE, code.message()))));
            responses.addApiResponse(
                    String.valueOf(status),
                    new ApiResponse()
                            .description(errorDescription(status))
                            .content(new Content().addMediaType(JSON, media)));
        });
    }

    private static void customizeSuccessResponse(
            String path, String feature, PathItem.HttpMethod method, ApiResponses responses) {
        String success = switch (method) {
            case POST -> "201";
            case DELETE -> "204";
            default -> "200";
        };
        if (!success.equals("200")) responses.remove("200");
        ApiResponse response = responses.get(success);
        if (success.equals("204")) response.setContent(null);
        else {
            response.setContent(jsonContent(response.getContent()));
            Object example = responseExample(feature);
            if (method == PathItem.HttpMethod.GET && !path.contains("{id}")) example = List.of(example);
            response.getContent().get(JSON).setExample(example);
        }
        if (success.equals("201"))
            response.addHeaderObject(
                    "Location",
                    new Header()
                            .description("Relative URL of the created resource")
                            .schema(new StringSchema().example(path + "/" + UUID_EXAMPLE)));
    }

    private static void customizeRequestBody(RequestBody body) {
        if (body == null) return;
        body.setRequired(true);
        body.setDescription("Required JSON object; only the documented fields are allowed. "
                + "Null required fields and scalar coercion are rejected by the custom decoder.");
        body.setContent(jsonContent(body.getContent()));
    }

    private static void customizePathParameters(List<Parameter> parameters) {
        if (parameters == null) return;
        parameters.forEach(parameter -> {
            if ("path".equals(parameter.getIn())) {
                parameter.setDescription("Canonical 36-character UUID text; upper/lowercase hex accepted");
                parameter.setExample(UUID_EXAMPLE);
                parameter
                        .getSchema()
                        .setPattern("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
            }
        });
    }

    private static Content jsonContent(Content content) {
        if (content == null || content.isEmpty()) return content;
        return new Content().addMediaType(JSON, content.values().iterator().next());
    }

    private static Map<String, Object> responseExample(String feature) {
        var customer = Map.<String, Object>of(
                "id", UUID_EXAMPLE, "name", "Demo Alice", "email", "alice@example.invalid", ACTIVE, true);
        if (feature.equals(CUSTOMERS)) return customer;
        if (feature.equals("products"))
            return Map.of("id", UUID_EXAMPLE, "name", "Demo Keyboard", "price", 50, ACTIVE, true);
        return Map.of(
                "id",
                UUID_EXAMPLE,
                "customerId",
                UUID_EXAMPLE,
                "customer",
                customer,
                "items",
                List.of(Map.of(
                        "productId",
                        UUID_EXAMPLE,
                        "productName",
                        "Demo Keyboard",
                        "quantity",
                        2,
                        "unitPrice",
                        50,
                        "lineTotal",
                        100)),
                "currency",
                "EUR",
                "subtotal",
                100,
                "discount",
                10,
                "total",
                90);
    }

    private static List<Code> errors(String path, String feature, PathItem.HttpMethod method) {
        var codes = new ArrayList<Code>();
        codes.addAll(List.of(NOT_ACCEPTABLE, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE));
        if (path.contains("{id}")) {
            codes.add(INVALID_RESOURCE_ID);
            codes.add(
                    switch (feature) {
                        case CUSTOMERS -> CUSTOMER_NOT_FOUND;
                        case "products" -> PRODUCT_NOT_FOUND;
                        default -> ORDER_NOT_FOUND;
                    });
        }
        boolean body = method == PathItem.HttpMethod.POST
                || method == PathItem.HttpMethod.PUT
                || method == PathItem.HttpMethod.PATCH;
        if (body) {
            codes.addAll(List.of(
                    MALFORMED_JSON,
                    UNKNOWN_FIELD,
                    UNSUPPORTED_FIELD,
                    INVALID_FIELD_TYPE,
                    REQUIRED_FIELD_MISSING,
                    UNSUPPORTED_MEDIA_TYPE));
            if (feature.equals("orders"))
                codes.addAll(List.of(
                        INVALID_RESOURCE_ID,
                        INVALID_ORDER_ITEMS,
                        INVALID_ORDER_QUANTITY,
                        CUSTOMER_NOT_FOUND,
                        PRODUCT_NOT_FOUND,
                        CUSTOMER_INACTIVE,
                        PRODUCT_INACTIVE));
            else if (method != PathItem.HttpMethod.PATCH) {
                codes.add(INVALID_NAME);
                codes.add(feature.equals(CUSTOMERS) ? INVALID_EMAIL : INVALID_PRODUCT_PRICE);
            }
        }
        if (feature.equals(CUSTOMERS) && (method == PathItem.HttpMethod.POST || method == PathItem.HttpMethod.PUT))
            codes.add(DUPLICATE_EMAIL);
        if (!feature.equals("orders")
                && (method == PathItem.HttpMethod.PUT
                        || method == PathItem.HttpMethod.PATCH
                        || method == PathItem.HttpMethod.DELETE)) codes.add(CONCURRENT_MODIFICATION);
        if (feature.equals(CUSTOMERS) && method == PathItem.HttpMethod.DELETE) codes.add(CUSTOMER_HAS_ORDERS);
        return codes;
    }

    private static String errorDescription(int status) {
        return switch (status) {
            case 400 -> "Invalid request; examples identify applicable exact code/message templates";
            case 404 -> "Referenced resource was not found";
            case 406 -> "Accept does not permit JSON; the error envelope still uses JSON";
            case 409 -> "Business or confirmed optimistic concurrency conflict; no blind replay";
            case 415 -> "Only application/json with UTF-8 is supported for request bodies";
            case 503 -> "Clearly classified temporary unavailability or confirmed-rollback transient exhaustion";
            default -> "Unexpected technical failure, including uncertain completion; rollback is not implied";
        };
    }
}
