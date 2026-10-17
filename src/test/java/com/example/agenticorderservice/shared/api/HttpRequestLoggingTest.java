package com.example.agenticorderservice.shared.api;

import static org.assertj.core.api.Assertions.*;

import ch.qos.logback.classic.*;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
        args = {"--spring.task.execution.pool.core-size=1", "--spring.task.execution.pool.max-size=1"})
@Import(HttpRequestLoggingTest.Fixture.class)
class HttpRequestLoggingTest extends ApiTestSupport {
    @LocalServerPort
    int port;

    final List<ILoggingEvent> events = new CopyOnWriteArrayList<>();
    final BlockingQueue<ILoggingEvent> terminal = new LinkedBlockingQueue<>();
    final Logger logger = (Logger) LoggerFactory.getLogger("com.example.agenticorderservice");
    final AppenderBase<ILoggingEvent> appender = new AppenderBase<>() {
        @Override
        protected void append(ILoggingEvent event) {
            event.prepareForDeferredProcessing();
            events.add(event);
            if (event.getFormattedMessage().startsWith("event=HTTP_REQUEST_COMPLETED")) terminal.add(event);
        }
    };

    Level previousLevel;

    @BeforeEach
    void capture() {
        previousLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detach() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(previousLevel);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"sync", "direct", "supplied", "redispatch", "cycles"})
    void actualContainerFinalizesOnceWithStableIdAndTransparentAsyncStart(String mode) throws Exception {
        var response = HttpClient.newHttpClient()
                .send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/logging-fixture?mode=" + mode))
                                .timeout(Duration.ofSeconds(10))
                                .header("X-Request-ID", "hostile-id")
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        String id = response.headers().firstValue("X-Request-ID").orElseThrow();
        assertThat(UUID.fromString(id)).hasToString(id);
        assertThat(response.body()).isEqualTo(id);
        var event = terminal.poll(10, TimeUnit.SECONDS);
        assertThat(event).isNotNull();
        assertThat(event.getMDCPropertyMap()).containsEntry("requestId", id);
        if (!mode.equals("sync"))
            assertThat(event.getFormattedMessage())
                    .contains("asyncCompleted=true", "cycles=" + (mode.equals("cycles") ? 2 : 1));
        assertThat(event.getFormattedMessage())
                .contains(mode.equals("direct") || mode.equals("supplied") ? "status=UNKNOWN" : "status=200");
        assertThat(events.stream()
                        .filter(e -> e.getFormattedMessage().startsWith("event=HTTP_REQUEST_COMPLETED"))
                        .count())
                .isEqualTo(1);
        assertThat(events)
                .allSatisfy(e ->
                        assertThat(e.getFormattedMessage()).doesNotContain("hostile-id", "logging-fixture", "mode="));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "/products,200",
        "/products/invalid-path-canary,400",
        "/unmapped-secret-path?password=canary,404",
        "/logging-fixture?mode=error,500",
        "/logging-fixture?mode=exception,500",
        "/logging-fixture?mode=timeout,200",
        "/logging-mvc/callable,200",
        "/logging-mvc/callable-failure,500",
        "/logging-mvc/deferred,200",
        "/logging-mvc/deferred-failure,500"
    })
    void realMvcAndContainerErrorAndTimeoutPathsKeepSafeTerminalOwnership(String path, int status) throws Exception {
        var response = HttpClient.newHttpClient()
                .send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                                .timeout(Duration.ofSeconds(10))
                                .header("X-Request-ID", "untrusted-canary")
                                .header("Cookie", "password=secret-canary")
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(status);
        var id = response.headers().firstValue("X-Request-ID").orElseThrow();
        var event = terminal.poll(10, TimeUnit.SECONDS);
        assertThat(event).isNotNull();
        assertThat(event.getMDCPropertyMap()).containsEntry("requestId", id);
        assertThat(event.getFormattedMessage()).contains("status=" + status);
        if (path.contains("timeout")) {
            assertThat(event.getFormattedMessage()).contains("outcome=COMPLETED_AFTER_TIMEOUT");
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
        }
        if (path.contains("callable") || path.contains("deferred")) {
            if (status == 200) assertThat(response.body()).isEqualTo(id);
            assertThat(context.getBean(AsyncEndpoints.class).observed.remove(id))
                    .isEqualTo(id);
            var delegate =
                    context.getBean("applicationTaskExecutor", org.springframework.core.task.AsyncTaskExecutor.class);
            assertThat(delegate.submit(() -> MDC.get("requestId")).get(5, TimeUnit.SECONDS))
                    .isNull();
        }
        if (status >= 400)
            ExpectedApiErrors.assertResponse(
                    status,
                    response.body(),
                    status == 404
                            ? "ENDPOINT_NOT_FOUND"
                            : status == 400 ? "INVALID_RESOURCE_ID" : "INTERNAL_SERVER_ERROR");
        assertThat(events.stream()
                        .filter(e -> e.getFormattedMessage().startsWith("event=HTTP_REQUEST_COMPLETED"))
                        .count())
                .isEqualTo(1);
        assertThat(events).allSatisfy(e -> assertThat(e.getThrowableProxy()).isNull());
        assertThat(events)
                .allSatisfy(e -> assertThat(e.getFormattedMessage())
                        .doesNotContain("canary", "password", "Cookie", "unmapped-secret-path"));
    }

    @org.springframework.web.bind.annotation.RestController
    static class AsyncEndpoints {
        final ConcurrentMap<String, String> observed = new ConcurrentHashMap<>();

        @org.springframework.web.bind.annotation.GetMapping("/logging-mvc/callable")
        Callable<String> callable() {
            String id = MDC.get("requestId");
            return () -> {
                observed.put(id, String.valueOf(MDC.get("requestId")));
                return id;
            };
        }

        @org.springframework.web.bind.annotation.GetMapping("/logging-mvc/callable-failure")
        Callable<String> callableFailure() {
            String id = MDC.get("requestId");
            return () -> {
                observed.put(id, String.valueOf(MDC.get("requestId")));
                throw new IllegalStateException("secret-canary");
            };
        }

        @org.springframework.web.bind.annotation.GetMapping({"/logging-mvc/deferred", "/logging-mvc/deferred-failure"})
        org.springframework.web.context.request.async.DeferredResult<String> deferred(HttpServletRequest request) {
            String id = MDC.get("requestId");
            boolean fail = request.getRequestURI().endsWith("failure");
            var result = new org.springframework.web.context.request.async.DeferredResult<String>();
            var handle = RequestCorrelationContext.capture();
            CompletableFuture.runAsync(handle.wrap(() -> {
                observed.put(id, String.valueOf(MDC.get("requestId")));
                if (fail) result.setErrorResult(new IllegalStateException("secret-canary"));
                else result.setResult(id);
            }));
            return result;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Fixture {
        @Bean
        AsyncEndpoints loggingAsyncEndpoints() {
            return new AsyncEndpoints();
        }

        @Bean
        ServletRegistrationBean<HttpServlet> loggingFixture() {
            var registration = new ServletRegistrationBean<HttpServlet>(
                    new HttpServlet() {
                        @Override
                        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                                throws IOException, ServletException {
                            String mode = request.getParameter("mode");
                            var state = (RequestLogState) request.getAttribute(RequestLogState.ATTRIBUTE);
                            assertThat(MDC.get("requestId")).isEqualTo(state.id);
                            if (request.getDispatcherType() == DispatcherType.ASYNC) {
                                if (mode.equals("cycles") && request.getAttribute("again") == null) {
                                    request.setAttribute("again", true);
                                    request.startAsync().dispatch();
                                    return;
                                }
                                response.getWriter().write(state.id);
                                return;
                            }
                            if (mode.equals("error")) {
                                response.sendError(500);
                                return;
                            }
                            if (mode.equals("exception")) throw new ServletException("fixture technical failure");
                            if (mode.equals("timeout")) {
                                var context = request.startAsync();
                                context.setTimeout(100);
                                context.addListener(new AsyncListener() {
                                    @Override
                                    public void onTimeout(AsyncEvent event) {
                                        event.getAsyncContext().dispatch();
                                    }

                                    @Override
                                    public void onError(AsyncEvent event) {
                                        // The production observer owns error recording; this fixture only initiates
                                        // timeout dispatch.
                                    }

                                    @Override
                                    public void onComplete(AsyncEvent event) {
                                        // The production observer and request listener own completion recording.
                                    }

                                    @Override
                                    public void onStartAsync(AsyncEvent event) {
                                        // This fixture callback does not register work for later async cycles.
                                    }
                                });
                                return;
                            }
                            if (mode.equals("sync")) {
                                response.getWriter().write(state.id);
                                return;
                            }
                            AsyncContext context = mode.equals("supplied")
                                    ? request.startAsync(request, response)
                                    : request.startAsync();
                            assertThat(context.getRequest())
                                    .isSameAs(
                                            mode.equals("supplied")
                                                    ? request
                                                    : ((HttpServletRequestWrapper) request).getRequest());
                            assertThat(context.getResponse()).isSameAs(response);
                            assertThat(state.cycle()).isEqualTo(1);
                            if (mode.equals("redispatch") || mode.equals("cycles")) context.dispatch();
                            else {
                                var handle = RequestCorrelationContext.capture();
                                context.start(handle.wrap(() -> {
                                    assertThat(MDC.get("requestId")).isEqualTo(state.id);
                                    try {
                                        context.getResponse().getWriter().write(state.id);
                                    } catch (IOException failure) {
                                        throw new IllegalStateException(failure);
                                    } finally {
                                        context.complete();
                                    }
                                }));
                            }
                        }
                    },
                    "/logging-fixture");
            registration.setAsyncSupported(true);
            return registration;
        }
    }
}
