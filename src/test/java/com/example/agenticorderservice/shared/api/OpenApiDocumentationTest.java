package com.example.agenticorderservice.shared.api;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("demo")
class OpenApiDocumentationTest extends ApiTestSupport {
    @Test
    void documentsOnlyImplementedFeatureOperationsAndActualSuccessCodes() throws Exception {
        var result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var spec = Json.mapper().readTree(result.getResponse().getContentAsByteArray());
        assertThat(spec.get("openapi").asText()).startsWith("3.");
        assertThat(spec.get("paths").properties()).hasSize(8);
        var paths = spec.get("paths");
        assertThat(paths.get("/customers").properties()).hasSize(2);
        assertThat(paths.get("/customers/{id}").properties()).hasSize(4);
        assertThat(paths.get("/products/{id}").properties()).hasSize(3);
        assertThat(paths.get("/orders").properties()).hasSize(2);
        assertThat(paths.get("/orders/{id}").properties()).hasSize(1);
        assertThat(paths.get("/products/{id}").has("delete")).isFalse();
        assertThat(paths.get("/customers").get("post").get("responses").has("201"))
                .isTrue();
        assertThat(paths.get("/customers/{id}")
                        .get("delete")
                        .get("responses")
                        .get("204")
                        .has("content"))
                .isFalse();
        assertThat(paths.get("/orders").get("post").get("responses").has("409")).isTrue();
        assertThat(paths.get("/orders/{id}").get("get").get("responses").has("404"))
                .isTrue();
        assertThat(spec.get("servers").get(0).get("url").asText()).isEqualTo("/");
        assertThat(spec.toString()).doesNotContain("OPERATION_NOT_AVAILABLE");
    }

    @Test
    void productImagesDocumentMultipartBinaryVariantsAndBodylessMutations() throws Exception {
        var response = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        var spec = Json.mapper().readTree(response.getContentAsByteArray());
        var image = spec.at("/paths/~1products~1{id}~1image");
        var part = image.at("/put/requestBody/content/multipart~1form-data/schema");
        assertThat(image.at("/put/requestBody/required").asBoolean()).isTrue();
        assertThat(part.path("required")).extracting(JsonNode::asText).contains("file");
        assertThat(part.at("/properties/file/type").asText()).isEqualTo("string");
        assertThat(part.at("/properties/file/format").asText()).isEqualTo("binary");
        for (String method : new String[] {"put", "delete"}) {
            assertThat(image.at("/" + method + "/responses/204").isMissingNode())
                    .isFalse();
            assertThat(image.at("/" + method + "/responses/204").has("content")).isFalse();
        }
        for (String media : new String[] {"image~1jpeg", "image~1png"}) {
            assertThat(image.at("/get/responses/200/content/" + media + "/schema/format")
                            .asText())
                    .isEqualTo("binary");
        }
        assertThat(spec.at(
                                "/paths/~1products~1{id}~1image~1thumbnail/get/responses/200/content/image~1png/schema/format")
                        .asText())
                .isEqualTo("binary");
        assertThat(image.at(
                                "/put/responses/403/content/application~1json/examples/PRODUCT_IMAGE_WRITES_DISABLED/value/code")
                        .asText())
                .isEqualTo("PRODUCT_IMAGE_WRITES_DISABLED");
    }

    @Test
    void customerDeleteDocumentsExactConflictAlongsideConcurrencyAndNotFound() throws Exception {
        var response = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        var spec = Json.mapper().readTree(response.getContentAsByteArray());
        var responses = spec.at("/paths/~1customers~1{id}/delete/responses");
        var conflict = responses.at("/409/content/application~1json");
        assertThat(conflict.at("/schema/$ref").asText()).isEqualTo("#/components/schemas/ApiError");
        assertThat(conflict.at("/examples/CUSTOMER_HAS_ORDERS/value/code").asText())
                .isEqualTo("CUSTOMER_HAS_ORDERS");
        assertThat(conflict.at("/examples/CUSTOMER_HAS_ORDERS/value/message").asText())
                .isEqualTo(
                        "Customer cannot be deleted because existing orders reference this customer. Deactivate the customer instead.");
        assertThat(conflict.at("/examples/CONCURRENT_MODIFICATION/value/code").asText())
                .isEqualTo("CONCURRENT_MODIFICATION");
        assertThat(responses
                        .at("/404/content/application~1json/examples/CUSTOMER_NOT_FOUND/value/code")
                        .asText())
                .isEqualTo("CUSTOMER_NOT_FOUND");
    }

    @Test
    void schemasPreserveStrictJsonAndDocumentBusinessConstraintsWithoutAddingValidation() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.CreateOrderRequest.additionalProperties")
                        .value(false))
                .andExpect(jsonPath("$.components.schemas.OrderItemRequest.properties.quantity.maximum")
                        .value(10000))
                .andExpect(jsonPath("$.components.schemas.ProductDetailsRequest.properties.price.maximum")
                        .value(999999.99))
                .andExpect(jsonPath("$.components.schemas.DeactivateCustomerRequest.properties.active.enum[0]")
                        .value(false))
                .andExpect(
                        jsonPath("$.paths['/orders'].post.requestBody.required").value(true))
                .andExpect(jsonPath("$.paths['/orders'].post.responses['201'].headers.X-Request-ID['$ref']")
                        .value("#/components/headers/RequestId"));
    }

    @Test
    void yamlAndJsonDescribeTheSameApiAndSwaggerUiIsAvailable() throws Exception {
        var json = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        var yaml = mvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        assertThat(Yaml.mapper().readTree(yaml)).isEqualTo(Json.mapper().readTree(json));
        var snapshot = Files.readAllBytes(Path.of("docs/02-architecture/openapi.yaml"));
        assertThat(Yaml.mapper().readTree(snapshot)).isEqualTo(Json.mapper().readTree(json));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/v3/api-docs"));
    }

    @Test
    void allLocalComponentReferencesResolveAndSharedErrorMatchesTheResponseContract() throws Exception {
        for (String route : new String[] {"/v3/api-docs", "/v3/api-docs.yaml"}) {
            var response = mvc.perform(get(route))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse();
            var mapper = route.endsWith(".yaml") ? Yaml.mapper() : Json.mapper();
            var spec = mapper.readTree(response.getContentAsByteArray());
            assertLocalComponentReferencesResolve(spec, spec);
            var error = spec.at("/components/schemas/ApiError");
            assertThat(error.path("type").asText()).isEqualTo("object");
            assertThat(error.path("properties").properties())
                    .extracting(java.util.Map.Entry::getKey)
                    .containsExactlyInAnyOrder("code", "message");
            assertThat(error.path("required"))
                    .extracting(JsonNode::asText)
                    .containsExactlyInAnyOrder("code", "message");
            assertThat(error.at("/properties/code/type").asText()).isEqualTo("string");
            assertThat(error.at("/properties/message/type").asText()).isEqualTo("string");
            assertThat(error.path("additionalProperties").asBoolean(true)).isFalse();
        }
    }

    private static void assertLocalComponentReferencesResolve(JsonNode root, JsonNode node) {
        var reference = node.path("$ref").asText();
        if (reference.startsWith("#/")) {
            assertThat(root.at(reference.substring(1)).isMissingNode())
                    .as("Unresolved local component reference: %s", reference)
                    .isFalse();
        }
        node.forEach(child -> assertLocalComponentReferencesResolve(root, child));
    }
}
