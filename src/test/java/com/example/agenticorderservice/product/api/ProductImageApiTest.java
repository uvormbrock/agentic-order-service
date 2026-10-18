package com.example.agenticorderservice.product.api;

import static com.example.agenticorderservice.product.image.ProductImageFixtures.*;
import static com.example.agenticorderservice.shared.api.ApiError.Code.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.agenticorderservice.product.application.*;
import com.example.agenticorderservice.shared.api.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;

class ProductImageApiTest extends ApiTestSupport {
    @Test
    void multipartPutDelegatesOriginalBytesAndDeleteReturnsNoContent() throws Exception {
        byte[] bytes = image("png", 640, 320);
        mvc.perform(multipart("/products/" + id + "/image")
                        .file(new MockMultipartFile("file", "../../ignored.png", "image/png", bytes))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(productImages).upload(id, bytes, "image/png");
        mvc.perform(delete("/products/" + id + "/image")).andExpect(status().isNoContent());
        verify(productImages).delete(id);
    }

    @Test
    void retrievesExactOriginalAndThumbnailWithSafeHeaders() throws Exception {
        byte[] original = image("jpg", 640, 320);
        byte[] thumbnail = image("png", 320, 160);
        when(productImages.get(id, false)).thenReturn(new ProductImageContent(original, "image/jpeg"));
        when(productImages.get(id, true)).thenReturn(new ProductImageContent(thumbnail, "image/png"));
        mvc.perform(get("/products/" + id + "/image").accept("image/jpeg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(original))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/products/" + id + "/image/thumbnail").accept("image/png"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(thumbnail));
    }

    @Test
    void refusesWrongRepresentationAndOversizedMultipartBeforeApplicationCall() throws Exception {
        when(productImages.get(id, false)).thenReturn(new ProductImageContent(image("jpg", 4, 4), "image/jpeg"));
        error(get("/products/" + id + "/image").accept("image/png"), NOT_ACCEPTABLE);
        error(get("/products/" + id + "/image").header("Accept", "image/jpeg;q=0,*/*;q=1"), NOT_ACCEPTABLE);
        imageError(
                multipart("/products/" + id + "/image")
                        .file(new MockMultipartFile("file", "big.png", "image/png", new byte[10 * 1024 * 1024 + 1]))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }),
                PRODUCT_IMAGE_TOO_LARGE);
        verify(productImages, never()).upload(any(), any(), any());
    }

    @ParameterizedTest
    @CsvSource({
        "INVALID,INVALID_PRODUCT_IMAGE",
        "TOO_LARGE,PRODUCT_IMAGE_TOO_LARGE",
        "UNSUPPORTED,UNSUPPORTED_PRODUCT_IMAGE",
        "WRITES_DISABLED,PRODUCT_IMAGE_WRITES_DISABLED",
        "STORAGE,INTERNAL_SERVER_ERROR"
    })
    void uploadFailuresUseExactEstablishedErrorEnvelope(String reason, String code) throws Exception {
        doThrow(new ProductImageFailure(ProductImageFailure.Reason.valueOf(reason)))
                .when(productImages)
                .upload(eq(id), any(), any());
        imageError(
                multipart("/products/" + id + "/image")
                        .file(new MockMultipartFile("file", "image.png", "image/png", image("png", 2, 2)))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }),
                com.example.agenticorderservice.shared.api.ApiError.Code.valueOf(code));
    }

    @Test
    void distinguishesMissingProductFromMissingImageAndRejectsInvalidPathAndPart() throws Exception {
        when(productImages.get(id, false)).thenThrow(new ProductNotFoundException());
        error(get("/products/" + id + "/image"), PRODUCT_NOT_FOUND);
        doThrow(new ProductImageFailure(ProductImageFailure.Reason.NOT_FOUND))
                .when(productImages)
                .get(id, false);
        imageError(get("/products/" + id + "/image"), PRODUCT_IMAGE_NOT_FOUND);
        error(get("/products/short/image"), INVALID_RESOURCE_ID);
        imageError(
                multipart("/products/" + id + "/image").with(request -> {
                    request.setMethod("PUT");
                    return request;
                }),
                REQUIRED_FIELD_MISSING);
        error(get("/products/" + id).accept("image/png"), NOT_ACCEPTABLE);
    }

    private void imageError(
            org.springframework.test.web.servlet.RequestBuilder request,
            com.example.agenticorderservice.shared.api.ApiError.Code code)
            throws Exception {
        mvc.perform(request)
                .andExpect(status().is(code.status()))
                .andExpect(content()
                        .json(context.getBean(tools.jackson.databind.json.JsonMapper.class)
                                .writeValueAsString(new com.example.agenticorderservice.shared.api.ApiError(code))));
    }
}
