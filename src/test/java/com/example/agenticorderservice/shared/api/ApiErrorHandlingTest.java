package com.example.agenticorderservice.shared.api;
import java.sql.*;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.example.agenticorderservice.shared.api.ApiError.Code.*;
class ApiErrorHandlingTest extends ApiTestSupport {
    @Test void incompatibleAcceptAndUnknownRouteStillRenderSafeJson() throws Exception {
        error(get("/products").accept("text/html"),NOT_ACCEPTABLE);
        error(get("/products").accept("application/json;q=0, */*;q=1"),NOT_ACCEPTABLE);
        error(get("/does-not-exist").accept("text/html"),ENDPOINT_NOT_FOUND);
        verifyNoInteractions(products, customers);
        mvc.perform(get("/products").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().json("[]"));
        verify(products,times(1)).list();
        verifyNoMoreInteractions(products);
        verifyNoInteractions(customers);
    }
    @Test void absentBodyWhitespaceNullAndDuplicateUseExactEnvelope() throws Exception {
        error(post("/products").contentType("application/json"),REQUIRED_FIELD_MISSING);
        error(post("/products"),REQUIRED_FIELD_MISSING);
        error(post("/products").contentType("application/json").content(" "),MALFORMED_JSON);
        error(post("/products").contentType("application/json").content("null"),MALFORMED_JSON);
        error(post("/products").contentType("application/json").content("{\"name\":1,\"name\":2}"),MALFORMED_JSON);
        verifyNoInteractions(products, customers);
    }
    @org.junit.jupiter.params.ParameterizedTest(name = "Spring exception={0}, wrapped={1}")
    @org.junit.jupiter.params.provider.CsvSource({"false, true", "false, false", "true, true", "true, false"})
    void confirmedCompletionFailuresTranslateOutsideServiceAndNeverReplay(boolean springException, boolean wrapped) throws Exception {
        RuntimeException conflict = springException
                ? new OptimisticLockingFailureException("private details")
                : new OptimisticLockException("private details");
        RuntimeException failure = wrapped ? new TransactionSystemException("secret sql",conflict) : conflict;
        when(products.deactivate(id)).thenThrow(failure);
        error(patch("/products/"+id).contentType("application/json").content("{\"active\":false}"),CONCURRENT_MODIFICATION);
        verify(products,times(1)).deactivate(id);
        verifyNoMoreInteractions(products);
        verifyNoInteractions(customers);
    }
    @Test void establishmentOutageAndAmbiguousCommitAreDifferent() throws Exception {
        when(products.list()).thenThrow(new CannotCreateTransactionException("offline",new SQLTransientConnectionException("pool unavailable")));
        error(get("/products"),SERVICE_UNAVAILABLE);
        doThrow(new TransactionSystemException("commit confirmation lost",new SQLException("secret", "08006"))).when(products).list();
        error(get("/products"),INTERNAL_SERVER_ERROR);
        assertThat(ApiExceptionHandler.classify(new SQLException("unique but unidentified", "23505"))).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(ApiExceptionHandler.classify(new IllegalArgumentException("programming defect"))).isEqualTo(INTERNAL_SERVER_ERROR);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.example.agenticorderservice.shared.api.StrictJsonRequestDecoderTest#invalid")
    void verifiesAllInputCategoriesThroughActualMvc(String body,ApiError.Code code) throws Exception {
        error(post("/products").contentType("application/json").content(body),code);
        verifyNoInteractions(products,customers);
    }
    @Test void patchesRejectEveryForbiddenAndIncorrectFieldAcrossBothFeatures() throws Exception {
        for(String feature:new String[]{"products","customers"}) {
            for(String forbidden:new String[]{"id","version","name",feature.equals("products")?"price":"email"}) {
                error(patch("/"+feature+"/"+id).contentType("application/json").content("{\"active\":false,\""+forbidden+"\":null}"),UNSUPPORTED_FIELD);
            }
            error(patch("/"+feature+"/"+id).contentType("application/json").content("{\"active\":true}"),UNSUPPORTED_FIELD);
            error(patch("/"+feature+"/"+id).contentType("application/json").content("{\"active\":\"false\"}"),INVALID_FIELD_TYPE);
            error(patch("/"+feature+"/"+id).contentType("application/json").content("{\"active\":null}"),REQUIRED_FIELD_MISSING);
            error(patch("/"+feature+"/"+id).contentType("application/json").content("{\"extra\":0}"),UNKNOWN_FIELD);
        }
        verifyNoInteractions(products,customers);
    }
    @Test void unrelatedIntegrityFailuresAndUnconfirmedTransactionErrorsRemainSafe500() throws Exception {
        doThrow(new org.springframework.dao.DataIntegrityViolationException("private check",new SQLException("private sql","23514"))).when(products).list();
        error(get("/products"),INTERNAL_SERVER_ERROR);
        assertThat(ApiExceptionHandler.classify(new CannotCreateTransactionException("configuration defect",new IllegalArgumentException("bad setup")))).isEqualTo(INTERNAL_SERVER_ERROR);
    }
    @Test void readOnlyServerFieldsAndMixedUuidCaseAreVerifiedOnMvcBoundary() throws Exception {
        error(post("/customers").contentType("application/json").content("{\"version\":1}"),UNSUPPORTED_FIELD);
        error(post("/customers").contentType("application/json").content("{\"name\":true,\"email\":false}"),INVALID_FIELD_TYPE);
        error(post("/products").accept("text/html").contentType("application/json").content("not json"),NOT_ACCEPTABLE);
        error(delete("/customers/not-a-uuid"),INVALID_RESOURCE_ID);
        mvc.perform(get("/products/aaaaAAAA-bBbB-cCcC-dDdD-eeEEeeEEeeEE")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> customerValueFailures() {
        return java.util.stream.Stream.of("POST", "PUT").flatMap(method ->
                java.util.stream.Stream.of(false, true).flatMap(reverse -> java.util.stream.Stream.of(
                        org.junit.jupiter.params.provider.Arguments.of(method, reverse, "", "valid@example.com", INVALID_NAME),
                        org.junit.jupiter.params.provider.Arguments.of(method, reverse, "Valid", "invalid", INVALID_EMAIL),
                        org.junit.jupiter.params.provider.Arguments.of(method, reverse, "", "invalid", INVALID_NAME))));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("customerValueFailures")
    void customerNamePrecedesEmailForBothWriteMethodsAndPropertyOrders(
            String method, boolean reverse, String name, String email, ApiError.Code expected) throws Exception {
        String nameField = "\"name\":\"" + name + "\"";
        String emailField = "\"email\":\"" + email + "\"";
        String body = "{" + (reverse ? emailField + "," + nameField : nameField + "," + emailField) + "}";
        var request = method.equals("POST") ? post("/customers") : put("/customers/" + id);
        error(request.contentType("application/json").content(body), expected);
        verifyNoInteractions(products, customers);
    }

    @Test
    void malformedContentTypeAndUnsupportedMethodPrecedeInvalidPathAndBody() throws Exception {
        error(put("/products/short").contentType("application").content("not json"), UNSUPPORTED_MEDIA_TYPE);
        error(delete("/products/short").contentType("application").accept("application").content("not json"), METHOD_NOT_ALLOWED);
        verifyNoInteractions(products, customers);
    }

    @Test
    void characterizesMalformedAcceptRejectionBeforePathAndBody() throws Exception {
        // Current malformed-header mapping, not a new selection of an unspecified Accept policy (AUD-05).
        error(put("/products/short").contentType("application/json").accept("application").content("not json"), NOT_ACCEPTABLE);
        verifyNoInteractions(products, customers);
    }

    @Test
    void nonemptyUntypedMvcBodyIsUnsupportedWhileAbsentBodyIsRequired() throws Exception {
        error(post("/products").content("{\"name\":\"A\",\"price\":1}"), UNSUPPORTED_MEDIA_TYPE);
        error(post("/products"), REQUIRED_FIELD_MISSING);
        verifyNoInteractions(products, customers);
    }
}
