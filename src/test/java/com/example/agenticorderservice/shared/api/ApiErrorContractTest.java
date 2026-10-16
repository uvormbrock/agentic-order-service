package com.example.agenticorderservice.shared.api;

import java.util.Arrays;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorContractTest {
    @Test
    void catalogContainsExactlyTheTwentyFiveApprovedCodes() {
        assertThat(ExpectedApiErrors.CATALOG).hasSize(25);
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

        var writer = new ApiErrorResponseWriter(JsonMapper.builder().build());
        for (boolean buffered : new boolean[]{false, true}) {
            var response = new MockHttpServletResponse();
            if (buffered) {
                response.getOutputStream().write("prior private content".getBytes(StandardCharsets.UTF_8));
            }
            writer.write(response, code);
            assertThat(response.getContentType()).startsWith("application/json");
            ExpectedApiErrors.assertResponse(response.getStatus(), response.getContentAsString(), name);
        }

        var committed = new MockHttpServletResponse();
        committed.setStatus(202);
        committed.setContentType("text/plain");
        committed.setHeader("X-Existing", "unchanged");
        committed.getOutputStream().write("already committed".getBytes(StandardCharsets.UTF_8));
        committed.flushBuffer();
        var originalContent = committed.getContentAsByteArray();
        var originalHeaderNames = List.copyOf(committed.getHeaderNames());
        writer.write(committed, code);
        assertThat(committed.isCommitted()).isTrue();
        assertThat(committed.getStatus()).isEqualTo(202);
        assertThat(committed.getContentType()).isEqualTo("text/plain");
        assertThat(committed.getHeader("X-Existing")).isEqualTo("unchanged");
        assertThat(committed.getHeaderNames()).containsExactlyInAnyOrderElementsOf(originalHeaderNames);
        assertThat(committed.getContentAsByteArray()).isEqualTo(originalContent);
    }
}
