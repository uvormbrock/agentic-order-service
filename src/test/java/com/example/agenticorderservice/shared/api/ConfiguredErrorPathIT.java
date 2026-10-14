package com.example.agenticorderservice.shared.api;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.web.error.path=/configured-error")
@Import(ConfiguredErrorPathIT.DispatchFixture.class)
class ConfiguredErrorPathIT extends PostgreSqlApiTestSupport {
    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"})
    void configuredServletDispatchPreservesStatusAndSafeJsonForEveryMethod(String method) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/test-dispatch/415"))
                .timeout(Duration.ofSeconds(20)).header("Accept", "text/html")
                .method(method, HttpRequest.BodyPublishers.noBody()).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(415);
        assertThat(response.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/json");
        if (method.equals("HEAD")) { assertThat(response.body()).isEmpty(); }
        else { assertError(response, ApiError.Code.UNSUPPORTED_MEDIA_TYPE); }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DispatchFixture {
        @Bean DispatchController dispatchController() { return new DispatchController(); }
    }

    @RestController
    static class DispatchController {
        @RequestMapping(path = "/test-dispatch/{status}", method = {
                RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH,
                RequestMethod.DELETE, RequestMethod.OPTIONS, RequestMethod.HEAD})
        void dispatch(@PathVariable int status, HttpServletResponse response) throws IOException {
            response.sendError(status); // Exercise the real embedded servlet container's ERROR dispatch.
        }
    }
}
