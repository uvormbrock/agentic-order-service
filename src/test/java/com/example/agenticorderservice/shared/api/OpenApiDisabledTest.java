package com.example.agenticorderservice.shared.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OpenApiDisabledTest extends ApiTestSupport {
    @ParameterizedTest
    @ValueSource(
            strings = {
                "/v3/api-docs",
                "/v3/api-docs.yaml",
                "/swagger-ui/index.html",
                "/v3/api-docs/swagger-config",
                "/swagger-ui.html",
                "/webjars/swagger-ui/index.html",
                "/webjars/swagger-ui/swagger-ui-bundle.js"
            })
    void documentationIsUnavailableWithoutTheDemoProfile(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isNotFound());
    }
}
