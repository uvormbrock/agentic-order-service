package com.example.agenticorderservice.shared.api;
import java.sql.*;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static com.example.agenticorderservice.shared.api.ApiError.Code.*;
class ApiErrorHandlingTest extends ApiTestSupport {
    @Test void incompatibleAcceptAndUnknownRouteStillRenderSafeJson() throws Exception {
        error(get("/products").accept("text/html"),NOT_ACCEPTABLE);
        error(get("/products").accept("application/json;q=0, */*;q=1"),NOT_ACCEPTABLE);
        error(get("/does-not-exist").accept("text/html"),ENDPOINT_NOT_FOUND);
        verifyNoInteractions(products, customers);
    }
    @Test void absentBodyWhitespaceNullAndDuplicateUseExactEnvelope() throws Exception {
        error(post("/products").contentType("application/json"),REQUIRED_FIELD_MISSING);
        error(post("/products"),REQUIRED_FIELD_MISSING);
        error(post("/products").contentType("application/json").content(" "),MALFORMED_JSON);
        error(post("/products").contentType("application/json").content("null"),MALFORMED_JSON);
        error(post("/products").contentType("application/json").content("{\"name\":1,\"name\":2}"),MALFORMED_JSON);
        verifyNoInteractions(products, customers);
    }
    @Test void confirmedCompletionFailuresTranslateOutsideServiceAndNeverReplay() throws Exception {
        when(products.deactivate(id)).thenThrow(new TransactionSystemException("secret sql",new OptimisticLockException("private details")));
        error(patch("/products/"+id).contentType("application/json").content("{\"active\":false}"),CONCURRENT_MODIFICATION);
        verify(products,times(1)).deactivate(id);
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
        error(delete("/customers/not-a-uuid"),OPERATION_NOT_AVAILABLE);
        mvc.perform(get("/products/aaaaAAAA-bBbB-cCcC-dDdD-eeEEeeEEeeEE")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }
}
