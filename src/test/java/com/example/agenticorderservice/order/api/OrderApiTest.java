package com.example.agenticorderservice.order.api;

import static com.example.agenticorderservice.shared.api.ApiError.Code.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.agenticorderservice.customer.application.CustomerResult;
import com.example.agenticorderservice.order.application.*;
import com.example.agenticorderservice.shared.api.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

class OrderApiTest extends ApiTestSupport {
    static final String ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    static final String VALID =
            "{\"customerId\":\"" + ID + "\",\"items\":[{\"productId\":\"" + ID + "\",\"quantity\":4}]}";

    OrderResult result() {
        return new OrderResult(
                id,
                id,
                new CustomerResult(id, "Current", "a@example.com", true),
                List.of(new OrderResult.Item(id, "Historical", 4, new BigDecimal("25.00"), new BigDecimal("100.00"))),
                "EUR",
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                new BigDecimal("90.00"));
    }

    @Test
    void allFourEndpointsUseExactSuccessContractsAndMaterializedFacts() throws Exception {
        when(placement.place(any())).thenReturn(result());
        when(orderQueries.get(id)).thenReturn(result());
        when(orderQueries.list()).thenReturn(List.of(result()));
        var created = mvc.perform(
                        post("/orders").contentType("application/json").content(VALID))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/orders/" + id))
                .andReturn()
                .getResponse();
        var tree = context.getBean(tools.jackson.databind.json.JsonMapper.class).readTree(created.getContentAsString());
        assertThat(tree.propertyNames())
                .containsExactlyInAnyOrder(
                        "id", "customerId", "customer", "items", "currency", "subtotal", "discount", "total");
        assertThat(tree.get("items").get(0).propertyNames())
                .containsExactlyInAnyOrder("productId", "productName", "quantity", "unitPrice", "lineTotal");
        assertThat(tree.get("customer").propertyNames()).containsExactlyInAnyOrder("id", "name", "email", "active");
        assertThat(tree.get("discount").decimalValue()).isEqualByComparingTo("10.00");
        mvc.perform(get("/orders/" + id))
                .andExpect(status().isOk())
                .andExpect(content().json(created.getContentAsString()));
        mvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].items[0].productName").value("Historical"));
        when(orderQueries.list()).thenReturn(List.of());
        mvc.perform(get("/orders")).andExpect(content().json("[]"));
        mvc.perform(delete("/customers/" + id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(customers).delete(id);
    }

    static Stream<Arguments> invalid() {
        return Stream.of(
                Arguments.of("", REQUIRED_FIELD_MISSING),
                Arguments.of("{}", REQUIRED_FIELD_MISSING),
                Arguments.of("null", MALFORMED_JSON),
                Arguments.of("[]", MALFORMED_JSON),
                Arguments.of(" ", MALFORMED_JSON),
                Arguments.of(VALID + " {}", MALFORMED_JSON),
                Arguments.of(VALID.replace("4}", "4,\"quantity\":4}"), MALFORMED_JSON),
                Arguments.of(VALID.replace("\"quantity\":4", "\"unitPrice\":1,\"quantity\":4"), UNSUPPORTED_FIELD),
                Arguments.of(VALID.replace("\"quantity\":4", "\"extra\":1,\"quantity\":\"bad\""), UNKNOWN_FIELD),
                Arguments.of(
                        VALID.replace("\"customerId\":\"" + ID + "\"", "\"customerId\":null"), REQUIRED_FIELD_MISSING),
                Arguments.of(VALID.replace("4}", "null}"), REQUIRED_FIELD_MISSING),
                Arguments.of(VALID.replace("4}", "0}"), INVALID_ORDER_QUANTITY),
                Arguments.of(VALID.replace("4}", "-1}"), INVALID_ORDER_QUANTITY),
                Arguments.of(VALID.replace("4}", "10001}"), INVALID_ORDER_QUANTITY),
                Arguments.of(VALID.replace("4}", "999999999999999999999}"), INVALID_ORDER_QUANTITY),
                Arguments.of(VALID.replace("4}", "1.5}"), INVALID_FIELD_TYPE),
                Arguments.of(VALID.replace("4}", "1.0}"), INVALID_FIELD_TYPE),
                Arguments.of(VALID.replace("4}", "1e0}"), INVALID_FIELD_TYPE),
                Arguments.of(VALID.replace("4}", "true}"), INVALID_FIELD_TYPE),
                Arguments.of(VALID.replace("4}", "\"4\"}"), INVALID_FIELD_TYPE),
                Arguments.of("{\"customerId\":\"" + ID + "\",\"items\":[]}", INVALID_ORDER_ITEMS),
                Arguments.of("{\"customerId\":\"" + ID + "\",\"items\":[null]}", REQUIRED_FIELD_MISSING),
                Arguments.of("{\"customerId\":\"" + ID + "\",\"items\":[1]}", INVALID_FIELD_TYPE),
                Arguments.of("{\"customerId\":\"" + ID + "\",\"items\":{}}", INVALID_FIELD_TYPE),
                Arguments.of(VALID.replace(ID, "bad"), INVALID_RESOURCE_ID),
                Arguments.of(
                        VALID.replace("4}]", "6000},{\"productId\":\"" + ID + "\",\"quantity\":4001}]"),
                        INVALID_ORDER_QUANTITY));
    }

    @ParameterizedTest
    @MethodSource("invalid")
    void strictNestedInputUsesIndependentExactEnvelopeBeforeApplication(String body, ApiError.Code code)
            throws Exception {
        error(post("/orders").contentType("application/json").content(body), code);
        verifyNoInteractions(placement, orderQueries, customers, products);
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "version", "customer", "currency", "subtotal", "discount", "total", "unitPrice"})
    void rejectsEveryRecognizedForbiddenRootField(String field) throws Exception {
        error(
                post("/orders")
                        .contentType("application/json")
                        .content(VALID.replaceFirst("\\{", "{\"" + field + "\":null,")),
                UNSUPPORTED_FIELD);
        verifyNoInteractions(placement);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "id",
                "version",
                "productName",
                "unitPrice",
                "lineTotal",
                "subtotal",
                "discount",
                "total",
                "currency"
            })
    void rejectsEveryRecognizedForbiddenItemField(String field) throws Exception {
        error(
                post("/orders")
                        .contentType("application/json")
                        .content(VALID.replace("\"quantity\":4", "\"" + field + "\":null,\"quantity\":4")),
                UNSUPPORTED_FIELD);
        verifyNoInteractions(placement);
    }

    static Stream<Arguments> precedence() {
        return Stream.of(
                Arguments.of(
                        "{\"customerId\":null,\"items\":[{\"productId\":null,\"quantity\":\"bad\"}]}",
                        INVALID_FIELD_TYPE),
                Arguments.of(
                        "{\"customerId\":\"bad\",\"items\":[{\"productId\":null,\"quantity\":0}]}",
                        REQUIRED_FIELD_MISSING),
                Arguments.of("{\"customerId\":\"bad\",\"items\":[]}", INVALID_RESOURCE_ID),
                Arguments.of(VALID.replace("\"quantity\":4", "\"productId\":\"bad\",\"quantity\":0"), MALFORMED_JSON),
                Arguments.of(
                        VALID.replace("\"quantity\":4", "\"quantity\":0,\"unitPrice\":1")
                                .replace("{\"customerId\"", "{\"extra\":1,\"customerId\""),
                        UNKNOWN_FIELD),
                Arguments.of(
                        VALID.replace("\"quantity\":4", "\"extra\":1,\"quantity\":0")
                                .replace("{\"customerId\"", "{\"id\":null,\"customerId\""),
                        UNSUPPORTED_FIELD),
                Arguments.of("{\"customerId\":\"" + ID + "\",\"items\":null}", REQUIRED_FIELD_MISSING),
                Arguments.of("{\"customerId\":12,\"items\":[]}", INVALID_FIELD_TYPE),
                Arguments.of(VALID.replace("\"productId\":\"" + ID + "\",", ""), REQUIRED_FIELD_MISSING),
                Arguments.of(VALID.replace(",\"quantity\":4", ""), REQUIRED_FIELD_MISSING));
    }

    @ParameterizedTest
    @MethodSource("precedence")
    void selectsTheAcceptedCategoryAndLexicographicPropertyFailure(String body, ApiError.Code code) throws Exception {
        error(post("/orders").contentType("application/json").content(body), code);
        verifyNoInteractions(placement);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10000})
    void acceptsInclusiveQuantityBoundariesThroughStrictMvc(int quantity) throws Exception {
        when(placement.place(any())).thenReturn(result());
        mvc.perform(post("/orders").contentType("application/json").content(VALID.replace(":4}", ":" + quantity + "}")))
                .andExpect(status().isCreated());
        var command = org.mockito.ArgumentCaptor.forClass(PlaceOrderCommand.class);
        verify(placement).place(command.capture());
        assertThat(command.getValue().consolidated()).containsEntry(UUID.fromString(ID), quantity);
    }

    @Test
    void acceptsDuplicateMixedCaseProductIdsAtConsolidatedMaximum() throws Exception {
        when(placement.place(any())).thenReturn(result());
        String body = VALID.replace(
                "4}]", "6000},{\"productId\":\"" + ID.toUpperCase(java.util.Locale.ROOT) + "\",\"quantity\":4000}]");
        mvc.perform(post("/orders").contentType("application/json").content(body))
                .andExpect(status().isCreated());
        var command = org.mockito.ArgumentCaptor.forClass(PlaceOrderCommand.class);
        verify(placement).place(command.capture());
        assertThat(command.getValue().consolidated()).hasSize(1).containsEntry(UUID.fromString(ID), 10000);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12", "true", "{}", "[]"})
    void rejectsEveryWrongProductIdShapeBeforeApplication(String spelling) throws Exception {
        error(
                post("/orders")
                        .contentType("application/json")
                        .content(VALID.replace("\"productId\":\"" + ID + "\"", "\"productId\":" + spelling)),
                INVALID_FIELD_TYPE);
        verifyNoInteractions(placement);
    }

    @Test
    void canonicalBodyIdsNormalizeBeforeConsolidationAndApplicationAccess() throws Exception {
        when(placement.place(any())).thenReturn(result());
        mvc.perform(post("/orders")
                        .contentType("application/json")
                        .content(VALID.replace(ID, ID.toUpperCase(java.util.Locale.ROOT))))
                .andExpect(status().isCreated());
        var command = org.mockito.ArgumentCaptor.forClass(PlaceOrderCommand.class);
        verify(placement).place(command.capture());
        assertThat(command.getValue().customerId()).isEqualTo(UUID.fromString(ID));
        assertThat(command.getValue().consolidated()).containsEntry(UUID.fromString(ID), 4);
    }

    @Test
    void precedenceMediaAndUnsupportedMethodsRemainSafe() throws Exception {
        error(get("/orders/bad").accept("text/html"), NOT_ACCEPTABLE);
        error(get("/orders/bad"), INVALID_RESOURCE_ID);
        error(post("/orders").contentType("application/problem+json").content(VALID), UNSUPPORTED_MEDIA_TYPE);
        error(post("/orders").contentType("application/json").content(new byte[] {(byte) 0xff}), MALFORMED_JSON);
        for (String method : List.of("PUT", "PATCH", "DELETE")) {
            var response = mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), "/orders/" + id))
                    .andReturn()
                    .getResponse();
            ExpectedApiErrors.assertResponse(response.getStatus(), response.getContentAsString(), "METHOD_NOT_ALLOWED");
            assertThat(response.getHeader("Allow"))
                    .contains("GET", "HEAD", "OPTIONS")
                    .doesNotContain("PUT", "PATCH", "DELETE");
        }
        verifyNoInteractions(placement, orderQueries);
    }

    @ParameterizedTest
    @EnumSource(
            value = OrderFailure.Reason.class,
            names = {"CUSTOMER_INACTIVE", "PRODUCT_INACTIVE", "UNAVAILABLE", "TECHNICAL"})
    void typedFailuresUseAcceptedSafeMessages(OrderFailure.Reason reason) throws Exception {
        when(placement.place(any())).thenThrow(new OrderFailure(reason));
        var code = switch (reason) {
            case CUSTOMER_INACTIVE -> CUSTOMER_INACTIVE;
            case PRODUCT_INACTIVE -> PRODUCT_INACTIVE;
            case UNAVAILABLE -> SERVICE_UNAVAILABLE;
            default -> INTERNAL_SERVER_ERROR;
        };
        error(post("/orders").contentType("application/json").content(VALID), code);
        verify(placement).place(any());
    }
}
