package com.example.agenticorderservice.product.api;

import static com.example.agenticorderservice.shared.api.ApiError.Code.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.agenticorderservice.product.application.*;
import com.example.agenticorderservice.shared.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

class ProductApiTest extends ApiTestSupport {
    @Test
    void listDelegatesOnlyToInputPortAndRetainsEveryResponseField() throws Exception {
        when(listProducts.list())
                .thenReturn(
                        java.util.List.of(new ProductResult(id, "Inactive", new java.math.BigDecimal("12.34"), false)));
        mvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content()
                        .json("[{\"id\":\"" + id + "\",\"name\":\"Inactive\",\"price\":12.34,\"active\":false}]"));
        verify(listProducts).list();
        verifyNoMoreInteractions(listProducts);
        verifyNoInteractions(getProduct, productWrites);
    }

    @Test
    void writeEndpointsDelegateNormalizedFactsAndPreserveExactResponses() throws Exception {
        var price = new java.math.BigDecimal("45.67");
        String body = "{\"name\":\"  Äpfel😀  \",\"price\":45.67}";
        mvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/products/" + id))
                .andExpect(
                        content().json("{\"id\":\"" + id + "\",\"name\":\"Äpfel😀\",\"price\":45.67,\"active\":true}"));
        mvc.perform(put("/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content()
                        .json("{\"id\":\"" + id + "\",\"name\":\"Äpfel😀\",\"price\":45.67,\"active\":false}"));
        mvc.perform(patch("/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(
                        content().json("{\"id\":\"" + id + "\",\"name\":\"Name\",\"price\":12.34,\"active\":false}"));
        verify(productWrites).create("Äpfel😀", price);
        verify(productWrites).update(id, "Äpfel😀", price);
        verify(productWrites).deactivate(id);
        verifyNoMoreInteractions(productWrites);
        verifyNoInteractions(products);
        verifyNoInteractions(getProduct);
    }

    @Test
    void missingWriteResourcesRetainFeatureErrorAndPhysicalDeleteRemainsUnsupported() throws Exception {
        when(productWrites.update(eq(id), anyString(), any())).thenThrow(new ProductNotFoundException());
        when(productWrites.deactivate(id)).thenThrow(new ProductNotFoundException());
        error(
                put("/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Valid\",\"price\":12.34}"),
                PRODUCT_NOT_FOUND);
        error(
                patch("/products/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"),
                PRODUCT_NOT_FOUND);
        verify(productWrites).update(id, "Valid", new java.math.BigDecimal("12.34"));
        verify(productWrites).deactivate(id);
        clearInvocations(productWrites);
        mvc.perform(delete("/products/" + id)).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(products, productWrites, getProduct);
    }

    @ParameterizedTest
    @ValueSource(strings = {"products", "customers"})
    void allTenEndpointsUseAcceptedBodiesStatusLocationAndInactiveRecords(String feature) throws Exception {
        String field = feature.equals("products") ? "\"price\":12.34" : "\"email\":\"user@example.com\"";
        String body = "{\"name\":\"Name\"," + field + "}";
        mvc.perform(post("/" + feature).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/" + feature + "/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.version").doesNotExist());
        mvc.perform(get("/" + feature))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        mvc.perform(get("/" + feature + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mvc.perform(put("/" + feature + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mvc.perform(patch("/" + feature + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"149", "149.9", "149.99", "0", "999999.99"})
    void acceptsExactDecimalPriceSpellings(String spelling) throws Exception {
        mvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Valid\",\"price\":" + spelling + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").isNumber());
    }

    @ParameterizedTest
    @ValueSource(strings = {"149.990", "10.000", "1.4999e2", "1e3", "-0.01", "1000000"})
    void rejectsPriceLexemesWithoutCallingApplication(String spelling) throws Exception {
        error(
                post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Valid\",\"price\":" + spelling + "}"),
                INVALID_PRODUCT_PRICE);
        verify(products, never()).create(anyString(), any());
        verify(productWrites, never()).create(anyString(), any());
    }

    @Test
    void contentHeadersThenPathThenBodyThenLookup() throws Exception {
        error(put("/products/short").contentType("application/xml").content("not json"), UNSUPPORTED_MEDIA_TYPE);
        error(put("/products/short").contentType(MediaType.APPLICATION_JSON).content("not json"), INVALID_RESOURCE_ID);
        error(put("/products/" + id).contentType(MediaType.APPLICATION_JSON).content("not json"), MALFORMED_JSON);
        when(getProduct.get(id)).thenThrow(new ProductNotFoundException());
        error(get("/products/" + id), PRODUCT_NOT_FOUND);
        verify(products, never()).update(any(), any(), any());
        verify(productWrites, never()).update(any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "application/merge-patch+json",
                "application/vendor+json",
                "application/json;charset=UTF-16",
                "text/plain"
            })
    void rejectsUnsupportedMedia(String media) throws Exception {
        error(post("/products").contentType(media).content("{}"), UNSUPPORTED_MEDIA_TYPE);
        verifyNoInteractions(products, productWrites, customers);
    }

    @Test
    void acceptsUpperCaseUuidAndRejectsShortenedUuid() throws Exception {
        mvc.perform(get("/products/" + id.toString().toUpperCase(java.util.Locale.ROOT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
        error(get("/products/1-1-1-1-1"), INVALID_RESOURCE_ID);
    }

    @Test
    void getDelegatesOnlyToInputPortAndPreservesExactInactiveResponse() throws Exception {
        mvc.perform(get("/products/" + id))
                .andExpect(status().isOk())
                .andExpect(
                        content().json("{\"id\":\"" + id + "\",\"name\":\"Name\",\"price\":12.34,\"active\":false}"));
        verify(getProduct).get(id);
        verifyNoInteractions(products);
    }

    @Test
    void invalidGetIdDoesNotInvokeEitherApplicationBoundary() throws Exception {
        error(get("/products/not-a-uuid"), INVALID_RESOURCE_ID);
        verifyNoInteractions(getProduct, products, productWrites);
    }

    @Test
    void strictConverterIsInputOnlyAndPrecedesDefaultReader() {
        var adapter = context.getBean(
                org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.class);
        var converter = adapter.getMessageConverters().stream()
                .filter(c -> c.canRead(ProductDetailsRequest.class, MediaType.APPLICATION_JSON))
                .findFirst()
                .orElseThrow();
        assertThat(converter).isInstanceOf(StrictRequestHttpMessageConverter.class);
        assertThat(converter.canWrite(ProductDetailsRequest.class, MediaType.APPLICATION_JSON))
                .isFalse();
        assertThat(adapter.getMessageConverters())
                .anyMatch(c -> c.canWrite(ProductResponse.class, MediaType.APPLICATION_JSON));
    }
}
