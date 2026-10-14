package com.example.agenticorderservice.shared.api;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
    @Autowired DispatchObservations observations;
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

    static Stream<Arguments> fallbackDispatches() {
        return Stream.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD").flatMap(method -> Stream.of(
                Arguments.of(method, 404, "ENDPOINT_NOT_FOUND"),
                Arguments.of(method, 405, "METHOD_NOT_ALLOWED"),
                Arguments.of(method, 406, "NOT_ACCEPTABLE"),
                Arguments.of(method, 500, "INTERNAL_SERVER_ERROR")));
    }

    @ParameterizedTest
    @MethodSource("fallbackDispatches")
    void observesActualErrorDispatchAndIndependentSafeFallbackForEveryMethod(String method, int status, String code) throws Exception {
        long productsBefore = jdbc.queryForObject("select count(*) from products", Long.class);
        long customersBefore = jdbc.queryForObject("select count(*) from customers", Long.class);
        String token = UUID.randomUUID().toString();
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/test-dispatch/" + status))
                .timeout(Duration.ofSeconds(20)).header("Accept", "text/html").header("X-Test-Dispatch", token)
                .method(method, HttpRequest.BodyPublishers.noBody()).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        var observation = observations.entries.remove(token);
        assertThat(observation).as("Servlet ERROR filter must observe this request").isNotNull();
        assertThat(observation.type()).isEqualTo(DispatcherType.ERROR);
        assertThat(observation.status()).isEqualTo(status);
        assertThat(observation.sourceMethod()).isEqualTo(method);
        assertThat(response.statusCode()).isEqualTo(ExpectedApiErrors.CATALOG.get(code).status());
        assertThat(response.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/json");
        if (method.equals("HEAD")) { assertThat(response.body()).isEmpty(); }
        else { ExpectedApiErrors.assertResponse(response.statusCode(), response.body(), code); }
        assertThat(jdbc.queryForObject("select count(*) from products", Long.class)).isEqualTo(productsBefore);
        assertThat(jdbc.queryForObject("select count(*) from customers", Long.class)).isEqualTo(customersBefore);
    }

    // The container may wrap ERROR dispatch as GET; record the real source request separately.
    record Observation(DispatcherType type, int status, String sourceMethod, String dispatchMethod) { }
    static class DispatchObservations {
        final ConcurrentHashMap<String, Observation> entries = new ConcurrentHashMap<>();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DispatchFixture {
        @Bean DispatchController dispatchController() { return new DispatchController(); }
        @Bean DispatchObservations dispatchObservations() { return new DispatchObservations(); }
        @Bean FilterRegistrationBean<Filter> errorDispatchObserver(DispatchObservations observations) {
            Filter observer = (request, response, chain) -> {
                var servlet = (HttpServletRequest) request;
                String token = servlet.getHeader("X-Test-Dispatch");
                if (token != null) {
                    observations.entries.put(token, new Observation(servlet.getDispatcherType(),
                            (Integer) servlet.getAttribute(RequestDispatcher.ERROR_STATUS_CODE),
                            (String) servlet.getAttribute("test.sourceMethod"), servlet.getMethod()));
                }
                chain.doFilter(request, response);
            };
            var registration = new FilterRegistrationBean<>(observer);
            registration.setDispatcherTypes(DispatcherType.ERROR);
            registration.addUrlPatterns("/*");
            return registration;
        }
    }

    @RestController
    static class DispatchController {
        @RequestMapping(path = "/test-dispatch/{status}", method = {
                RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH,
                RequestMethod.DELETE, RequestMethod.OPTIONS, RequestMethod.HEAD})
        void dispatch(@PathVariable int status, HttpServletRequest request, HttpServletResponse response) throws IOException {
            request.setAttribute("test.sourceMethod", request.getMethod());
            response.sendError(status, "private SQL java.sql.SQLException stack trace");
        }
    }
}
