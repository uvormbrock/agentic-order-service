package com.example.agenticorderservice.shared.api;

import static org.assertj.core.api.Assertions.*;

import com.example.agenticorderservice.shared.logging.EventCapture;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.error.ErrorPage;
import org.springframework.boot.web.error.ErrorPageRegistrar;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration")
@Import(HttpRequestLifecycleIT.Fixture.class)
class HttpRequestLifecycleIT extends ApiTestSupport {
    @LocalServerPort
    int port;

    @Autowired
    Probes probes;

    static class Probe {
        final CountDownLatch entered = new CountDownLatch(1),
                release = new CountDownLatch(1),
                callback = new CountDownLatch(1),
                completed = new CountDownLatch(1),
                recovery = new CountDownLatch(1),
                errorDispatch = new CountDownLatch(1);
        volatile String requestId, nestedBefore, nestedAfter;
        volatile boolean restartedIdentity;
        volatile boolean mutableAtRecoveryBoundary;
        final AtomicInteger errors = new AtomicInteger(), completions = new AtomicInteger();
        final AtomicInteger restarts = new AtomicInteger(), registrations = new AtomicInteger();
        final Queue<String> dispatchRequestIds = new ConcurrentLinkedQueue<>();
        final Queue<RuntimeException> callbackFailures = new ConcurrentLinkedQueue<>();
        final Queue<Boolean> dispatchScopeRestored = new ConcurrentLinkedQueue<>();
    }

    static class Probes {
        final ConcurrentMap<String, Probe> values = new ConcurrentHashMap<>();
    }

    static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

    static HttpRequest request(int port, String mode, String key) {
        return HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + port + "/lifecycle?mode=" + mode + "&key=" + key))
                .timeout(Duration.ofSeconds(15))
                .build();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "gate",
                "supplied",
                "timeout-complete",
                "timeout-error",
                "injected-disconnect",
                "direct",
                "committed",
                "reset",
                "nested"
            })
    void actualServletLifecyclePreservesConservativeStatusAndOneTerminalAttempt(String mode) throws Exception {
        verifyLifecycle(mode);
    }

    @Test
    void nativeUnhandledAsyncErrorCompletesWithoutFalseSuccess() throws Exception {
        verifyLifecycle("native-error");
    }

    @Test
    void preCommitAsyncRecoveryPreservesObservedFailure() throws Exception {
        verifyLifecycle("precommit-recovery");
    }

    private void verifyLifecycle(String mode) throws Exception {
        String key = UUID.randomUUID().toString();
        var probe = new Probe();
        probes.values.put(key, probe);
        try (var events = new EventCapture("com.example.agenticorderservice")) {
            var future = HttpClient.newHttpClient()
                    .sendAsync(request(port, mode, key), HttpResponse.BodyHandlers.ofString());
            await(probe.entered);
            if (Set.of("gate", "supplied", "native-error", "precommit-recovery", "injected-disconnect")
                    .contains(mode)) {
                assertThat(events.messages()).noneMatch(m -> m.contains("HTTP_REQUEST_COMPLETED"));
                assertThat(future.isDone()).isFalse();
                probe.release.countDown();
            }
            var response = future.get(15, TimeUnit.SECONDS);
            if (mode.equals("timeout-error")) await(probe.completed);
            var terminal = events.await(probe.requestId, "HTTP_REQUEST_COMPLETED");
            assertThat(response.headers().firstValue("X-Request-ID")).contains(probe.requestId);
            assertThat(UUID.fromString(probe.requestId)).hasToString(probe.requestId);
            String message = terminal.getFormattedMessage();
            if (Set.of("gate", "supplied", "timeout-complete", "injected-disconnect", "direct")
                    .contains(mode)) assertThat(message).contains("status=UNKNOWN", "outcome=UNKNOWN");
            else if (mode.equals("native-error")) {
                assertThat(response.statusCode()).isEqualTo(500);
                assertThat(probe.dispatchRequestIds).hasSize(2).containsOnly(probe.requestId);
                assertThat(probe.restarts.get()).isZero();
                assertThat(probe.registrations.get()).isEqualTo(1);
                assertThat(probe.callbackFailures).isEmpty();
                // onError follows container error rendering; no later dispatch establishes a status.
                assertThat(message).contains("status=UNKNOWN", "outcome=UNKNOWN", "cycles=1");
            } else if (mode.equals("precommit-recovery")) {
                await(probe.recovery);
                assertThat(response.statusCode()).isEqualTo(502);
                assertThat(response.body()).isEqualTo("recovered");
                assertThat(probe.dispatchRequestIds).hasSize(3).containsOnly(probe.requestId);
                assertThat(probe.restarts.get()).isEqualTo(1);
                assertThat(probe.registrations.get()).isEqualTo(2);
                assertThat(probe.restartedIdentity).isTrue();
                assertThat(probe.mutableAtRecoveryBoundary).isTrue();
                assertThat(probe.errors.get()).isZero();
                assertThat(probe.callbackFailures).isEmpty();
                assertThat(message).contains("status=502", "outcome=COMPLETED_AFTER_ERROR");
                assertThat(message).contains("cycles=2");
            } else if (mode.equals("timeout-error")) {
                await(probe.errorDispatch);
                assertThat(response.statusCode()).isEqualTo(503);
                assertThat(response.body()).isEqualTo("timeout");
                assertThat(message).contains("status=503", "outcome=COMPLETED_AFTER_TIMEOUT");
                assertThat(probe.errors.get()).isZero();
                assertThat(probe.callbackFailures).isEmpty();
            } else if (mode.equals("committed")) assertThat(message).contains("status=202");
            else if (mode.equals("reset")) assertThat(message).contains("status=201");
            else {
                await(probe.errorDispatch);
                assertThat(message).contains("status=500");
                assertThat(probe.nestedBefore).isEqualTo(probe.requestId);
                assertThat(probe.nestedAfter).isEqualTo(probe.requestId);
            }
            if (!Set.of("committed", "reset", "nested").contains(mode)) {
                assertThat(message).contains("asyncCompleted=true");
                assertThat(probe.completions.get()).isEqualTo(1);
            }
            if (mode.equals("native-error") || mode.equals("injected-disconnect"))
                assertThat(probe.errors.get()).isEqualTo(1);
            if (mode.equals("native-error") || mode.equals("precommit-recovery"))
                assertThat(probe.dispatchScopeRestored).isNotEmpty().containsOnly(true);
            assertThat(events.events.stream()
                            .filter(e ->
                                    probe.requestId.equals(e.getMDCPropertyMap().get("requestId"))
                                            && e.getFormattedMessage().startsWith("event=HTTP_REQUEST_COMPLETED "))
                            .count())
                    .isEqualTo(1);
            assertThat(events.messages())
                    .allSatisfy(m -> assertThat(m).doesNotContain("transport-canary", "mode=", "key=", "/lifecycle"));
            assertThat(events.events)
                    .allSatisfy(e -> assertThat(e.getThrowableProxy()).isNull());
        } finally {
            probe.release.countDown();
            probes.values.remove(key);
        }
    }

    @Test
    void gracefulShutdownWaitsForActiveAsyncRequestWithoutPrematureCompletion() throws Exception {
        var probe = new Probe();
        var ownProbes = new Probes();
        String key = UUID.randomUUID().toString();
        ownProbes.values.put(key, probe);
        var factory = new org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory(0);
        factory.setShutdown(org.springframework.boot.web.server.Shutdown.GRACEFUL);
        var server = factory.getWebServer(context -> {
            new ServletListenerRegistrationBean<>(new RequestLoggingListener()).onStartup(context);
            var filter = new FilterRegistrationBean<>(new RequestCorrelationFilter());
            filter.setAsyncSupported(true);
            filter.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
            filter.onStartup(context);
            var servlet = new ServletRegistrationBean<>(new NativeServlet(ownProbes), "/lifecycle");
            servlet.setAsyncSupported(true);
            servlet.onStartup(context);
        });
        var shutdown = new CountDownLatch(1);
        try (var events = new EventCapture(RequestLoggingListener.class)) {
            server.start();
            var response = HttpClient.newHttpClient()
                    .sendAsync(request(server.getPort(), "gate", key), HttpResponse.BodyHandlers.ofString());
            await(probe.entered);
            server.shutDownGracefully(result -> shutdown.countDown());
            assertThat(shutdown.getCount()).isEqualTo(1);
            assertThat(events.messages()).isEmpty();
            probe.release.countDown();
            assertThat(response.get(15, TimeUnit.SECONDS).statusCode()).isEqualTo(200);
            assertThat(events.await(probe.requestId, "HTTP_REQUEST_COMPLETED").getFormattedMessage())
                    .contains("status=UNKNOWN");
            await(shutdown);
            assertThat(probe.completions.get()).isEqualTo(1);
        } finally {
            probe.release.countDown();
            server.stop();
            server.destroy();
        }
    }

    static class NativeServlet extends HttpServlet {
        final Probes probes;

        NativeServlet(Probes probes) {
            this.probes = probes;
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            Probe probe = probes.values.get(request.getParameter("key"));
            request.setAttribute("lifecycle.probe", probe);
            String mode = request.getParameter("mode");
            probe.requestId = MDC.get("requestId");
            if (mode.equals("native-error") || mode.equals("precommit-recovery"))
                probe.dispatchRequestIds.add(probe.requestId);
            probe.entered.countDown();
            if (request.getDispatcherType() == DispatcherType.ASYNC) {
                if (probe.errors.get() == 0 && mode.equals("native-error")) {
                    throw new ServletException("transport-canary");
                }
                if (mode.equals("precommit-recovery") && probe.restarts.get() == 0)
                    throw new ServletException("transport-canary");
                if (probe.errors.get() == 0 && mode.equals("injected-disconnect"))
                    throw new IOException("transport-canary");
                response.setStatus(502);
                response.getWriter().write("recovered");
                if (mode.equals("precommit-recovery")) probe.recovery.countDown();
                return;
            }
            if (mode.equals("committed")) {
                response.setStatus(202);
                response.getWriter().write("committed");
                response.flushBuffer();
                return;
            }
            if (mode.equals("reset")) {
                response.reset();
                response.setStatus(201);
                response.getWriter().write("reset");
                return;
            }
            if (mode.equals("nested")) {
                response.sendError(500);
                return;
            }
            AsyncContext async = mode.equals("supplied") ? request.startAsync(request, response) : request.startAsync();
            async.addListener(new AsyncListener() {
                @Override
                public void onComplete(AsyncEvent event) {
                    probe.completions.incrementAndGet();
                    probe.completed.countDown();
                }

                @Override
                public void onStartAsync(AsyncEvent event) {
                    if (mode.equals("precommit-recovery")) {
                        try {
                            probe.restarts.incrementAndGet();
                            event.getAsyncContext().addListener(this);
                            probe.registrations.incrementAndGet();
                        } catch (RuntimeException failure) {
                            probe.callbackFailures.add(failure);
                            throw failure;
                        }
                    }
                }

                @Override
                public void onTimeout(AsyncEvent event) throws IOException {
                    probe.callback.countDown();
                    // Render the test-owned error page before completion, without a second async dispatch.
                    if (mode.equals("timeout-error")) {
                        ((HttpServletResponse) event.getAsyncContext().getResponse()).sendError(503);
                    }
                    event.getAsyncContext().complete();
                }

                @Override
                public void onError(AsyncEvent event) {
                    probe.errors.incrementAndGet();
                    probe.callback.countDown();
                    try {
                        if (mode.equals("injected-disconnect") || mode.equals("native-error"))
                            event.getAsyncContext().complete();
                        else event.getAsyncContext().dispatch();
                    } catch (RuntimeException failure) {
                        probe.callbackFailures.add(failure);
                        throw failure;
                    }
                }
            });
            if (mode.equals("native-error") || mode.equals("precommit-recovery")) probe.registrations.incrementAndGet();
            if (mode.startsWith("timeout")) {
                async.setTimeout(50);
                return;
            }
            if (mode.equals("direct")) {
                async.complete();
                return;
            }
            var handle = RequestCorrelationContext.capture();
            async.start(handle.wrap(() -> {
                await(probe.release);
                if (Set.of("native-error", "precommit-recovery", "injected-disconnect")
                        .contains(mode)) {
                    async.dispatch();
                    return;
                }
                try {
                    async.getResponse().getWriter().write("done");
                } catch (IOException failure) {
                    throw new UncheckedIOException(failure);
                } finally {
                    async.complete();
                }
            }));
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Fixture {
        @Bean
        Probes lifecycleProbes() {
            return new Probes();
        }

        @Bean
        ServletRegistrationBean<NativeServlet> lifecycleServlet(Probes probes) {
            var registration = new ServletRegistrationBean<>(new NativeServlet(probes), "/lifecycle");
            registration.setAsyncSupported(true);
            return registration;
        }

        @Bean
        ErrorPageRegistrar timeoutErrorPage() {
            // The production fallback intentionally maps unsupported error-page statuses to 500.
            return registry -> registry.addErrorPages(
                    new ErrorPage(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "/lifecycle-timeout-error"));
        }

        @Bean
        ServletRegistrationBean<HttpServlet> timeoutErrorServlet() {
            var servlet = new HttpServlet() {
                @Override
                protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
                    assertThat(request.getDispatcherType()).isEqualTo(DispatcherType.ERROR);
                    assertThat(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE))
                            .isEqualTo(503);
                    response.setStatus(503);
                    response.getWriter().write("timeout");
                }
            };
            return new ServletRegistrationBean<>(servlet, "/lifecycle-timeout-error");
        }

        @Bean
        FilterRegistrationBean<Filter> preCommitRecoveryBoundary() {
            Filter boundary = (request, response, chain) -> {
                String previousId = MDC.get("requestId");
                try {
                    chain.doFilter(request, response);
                } catch (ServletException failure) {
                    if (request.getDispatcherType() != DispatcherType.ASYNC
                            || !"precommit-recovery".equals(request.getParameter("mode"))) throw failure;
                    Probe probe = (Probe) request.getAttribute("lifecycle.probe");
                    probe.mutableAtRecoveryBoundary = !response.isCommitted();
                    // The inner production filter has already observed the genuine failed dispatch.
                    AsyncContext restarted = request.startAsync(request, response);
                    probe.restartedIdentity = restarted.getRequest() == request && restarted.getResponse() == response;
                    restarted.dispatch();
                } finally {
                    if (request.getAttribute("lifecycle.probe") instanceof Probe probe
                            && Set.of("native-error", "precommit-recovery").contains(request.getParameter("mode")))
                        probe.dispatchScopeRestored.add(Objects.equals(previousId, MDC.get("requestId")));
                }
            };
            var registration = new FilterRegistrationBean<>(boundary);
            registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
            registration.setAsyncSupported(true);
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 9);
            return registration;
        }

        @Bean
        FilterRegistrationBean<Filter> nestedErrorProbe() {
            Filter observer = (request, response, chain) -> {
                if (request.getAttribute("lifecycle.probe") instanceof Probe probe) {
                    probe.errorDispatch.countDown();
                    probe.nestedBefore = MDC.get("requestId");
                    // Exercise nested correlation scoping inside a genuine Tomcat ERROR dispatch.
                    new RequestCorrelationFilter().doFilter(request, response, chain);
                    probe.nestedAfter = MDC.get("requestId");
                } else chain.doFilter(request, response);
            };
            var registration = new FilterRegistrationBean<>(observer);
            registration.setDispatcherTypes(DispatcherType.ERROR);
            registration.setAsyncSupported(true);
            return registration;
        }
    }
}
