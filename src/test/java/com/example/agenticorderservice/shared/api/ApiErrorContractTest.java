package com.example.agenticorderservice.shared.api;

import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorContractTest {
    @Test
    void catalogContainsExactlyTheTwentyApprovedCodes() {
        assertThat(ExpectedApiErrors.CATALOG).hasSize(20);
        assertThat(Arrays.stream(ApiError.Code.values()).map(Enum::name).toList())
                .containsExactlyInAnyOrderElementsOf(ExpectedApiErrors.CATALOG.keySet());
    }

    static Stream<String> approvedCodes() { return ExpectedApiErrors.CATALOG.keySet().stream(); }

    @ParameterizedTest
    @MethodSource("approvedCodes")
    void writesIndependentExactStatusAndSafeEnvelopeForEveryApprovedCode(String name) throws Exception {
        var expected = ExpectedApiErrors.CATALOG.get(name);
        var code = ApiError.Code.valueOf(name);
        assertThat(code.name()).isEqualTo(name);
        assertThat(code.status()).isEqualTo(expected.status());
        assertThat(code.message()).isEqualTo(expected.message());

        var response = new MockHttpServletResponse();
        new ApiErrorResponseWriter(JsonMapper.builder().build()).write(response, code);
        assertThat(response.getContentType()).startsWith("application/json");
        ExpectedApiErrors.assertResponse(response.getStatus(), response.getContentAsString(), name);
    }
}
