package com.example.agenticorderservice.shared.api;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.*;
import org.springframework.web.context.request.*;

class RequestLogStateTest {
    @Test
    void unresolvedAndLateObservationsCannotFabricateSuccess() {
        var state = new RequestLogState();
        long outer = state.enter("GET");
        state.observe(outer, 200, true, false, "/orders");
        state.failure(false);
        var result = state.close();
        assertThat(result.status()).isZero();
        assertThat(result.outcome()).isEqualTo("UNKNOWN");
        assertThat(result.pending()).isEqualTo("ASYNC_ERROR");
        state.observe(999, 200, true, false, "/products");
        state.failure(true);
        assertThat(state.close()).isNull();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void newerErrorResolutionWinsOverStaleOuterUnwind(boolean timeout) {
        var state = new RequestLogState();
        long outer = state.enter("GET");
        state.failure(timeout);
        long error = state.enter("GET");
        state.observe(error, 503, true, false, "/orders");
        state.observe(outer, 200, true, false, "/products");
        var result = state.close();
        assertThat(result.status()).isEqualTo(503);
        assertThat(result.route()).isEqualTo("/orders");
        assertThat(result.outcome()).isEqualTo(timeout ? "COMPLETED_AFTER_TIMEOUT" : "COMPLETED_AFTER_ERROR");
    }

    @Test
    void duplicateClosureAndDiagnosticClaimsAreAtomic() throws Exception {
        var state = new RequestLogState();
        state.failure(true);
        var barrier = new CyclicBarrier(8);
        try (var pool = Executors.newFixedThreadPool(8)) {
            var tasks = new ArrayList<Future<RequestLogState.Snapshot>>();
            for (int i = 0; i < 8; i++)
                tasks.add(pool.submit(() -> {
                    barrier.await(5, TimeUnit.SECONDS);
                    return state.close();
                }));
            int winners = 0;
            for (var task : tasks) if (task.get(5, TimeUnit.SECONDS) != null) winners++;
            assertThat(winners).isEqualTo(1);
        }
        assertThat(state.claimDiagnostic()).isFalse();
        var next = new RequestLogState();
        assertThat(next.claimDiagnostic()).isTrue();
        assertThat(next.claimDiagnostic()).isFalse();
        next.failure(false);
        assertThat(next.close().pending()).isEqualTo("NONE");
    }

    @Test
    void ownedMdcRestoresNestedAndUnrelatedContextOnSuccessAndFailure() throws Exception {
        String old = MDC.get("requestId"), other = MDC.get("unrelated");
        var request = new MockHttpServletRequest();
        var state = new RequestLogState();
        request.setAttribute(RequestLogState.ATTRIBUTE, state);
        var previous = RequestContextHolder.getRequestAttributes();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            MDC.put("requestId", "outer");
            MDC.put("unrelated", "preserved");
            var handle = RequestCorrelationContext.capture();
            handle.wrap((Runnable) () -> assertThat(MDC.get("requestId")).isEqualTo(state.id))
                    .run();
            assertThat(MDC.get("requestId")).isEqualTo("outer");
            var failure = new IllegalStateException("private-canary");
            assertThatThrownBy(() -> handle.wrap((Runnable) () -> {
                                throw failure;
                            })
                            .run())
                    .isSameAs(failure);
            assertThat(MDC.get("requestId")).isEqualTo("outer");
            assertThat(MDC.get("unrelated")).isEqualTo("preserved");
            state.close();
            handle.wrap((Runnable) () -> assertThat(MDC.get("requestId")).isEqualTo("outer"))
                    .run();
            assertThat(MDC.get("requestId")).isEqualTo("outer");
        } finally {
            RequestContextHolder.setRequestAttributes(previous);
            if (old == null) MDC.remove("requestId");
            else MDC.put("requestId", old);
            if (other == null) MDC.remove("unrelated");
            else MDC.put("unrelated", other);
        }
    }

    @Test
    void wrapperDelegatesExactOverloadsAndPreservesExceptionsAndObserverFailure() {
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        var response = mock(jakarta.servlet.http.HttpServletResponse.class);
        var context = mock(AsyncContext.class);
        var state = new RequestLogState();
        var wrapper = new RequestCorrelationFilter.ObservedRequest(request, state);
        when(request.startAsync()).thenReturn(context);
        assertThat(wrapper.startAsync()).isSameAs(context);
        verify(request).startAsync();
        verify(context).addListener(any(RequestCorrelationFilter.Observer.class));
        var next = new RequestCorrelationFilter.ObservedRequest(request, new RequestLogState());
        when(request.startAsync(request, response)).thenReturn(context);
        assertThat(next.startAsync(request, response)).isSameAs(context);
        verify(request).startAsync(request, response);
        var unsupported = new IllegalStateException("original");
        when(request.startAsync()).thenThrow(unsupported);
        assertThatThrownBy(wrapper::startAsync).isSameAs(unsupported);
        var brokenState = new RequestLogState();
        var broken = new RequestCorrelationFilter.ObservedRequest(request, brokenState);
        doReturn(context).when(request).startAsync();
        doThrow(new IllegalStateException("observer")).when(context).addListener(any());
        assertThat(broken.startAsync()).isSameAs(context);
        assertThat(brokenState.close().outcome()).isEqualTo("UNKNOWN");
    }

    @Test
    void callbacksNeverReadCompletedResponseOrEmitTerminalAndRegisterEachNewCycle() throws Exception {
        var state = new RequestLogState();
        var context = mock(AsyncContext.class);
        var observer = new RequestCorrelationFilter.Observer(state, 0);
        var event = new AsyncEvent(context);
        observer.onTimeout(event);
        observer.onError(event);
        observer.onComplete(event);
        verifyNoInteractions(context);
        observer.onStartAsync(event);
        var registrations = org.mockito.ArgumentCaptor.forClass(AsyncListener.class);
        verify(context).addListener(registrations.capture());
        assertThat(state.cycle()).isEqualTo(1);
        observer.onStartAsync(event); // Duplicate callback from generation zero.
        verify(context, times(1)).addListener(any());
        assertThat(state.cycle()).isEqualTo(1);
        // The next generation's distinct observer accepts a genuinely new cycle,
        // even when the container reuses its AsyncContext instance.
        registrations.getValue().onStartAsync(event);
        verify(context, times(2)).addListener(any());
        assertThat(state.cycle()).isEqualTo(2);
        assertThat(state.startCycle(0)).isZero();
        assertThat(state.close().pending()).isEqualTo("ASYNC_ERROR");
    }

    @Test
    void metadataFailureAndUnobservedScopeRemainUnknown() {
        var state = new RequestLogState();
        long token = state.enter("HOSTILE\r\nHEADER");
        state.observe(token, 200, true, false, "/private/email@example.com");
        state.incomplete();
        var snapshot = state.close();
        assertThat(snapshot.method()).isEqualTo("OTHER");
        assertThat(snapshot.route()).isEqualTo("OTHER");
        assertThat(snapshot.status()).isZero();
        assertThat(snapshot.outcome()).isEqualTo("UNKNOWN");
    }

    @Test
    void workerScopesRestoreReusedThreadsAndRetainRunningContextUntilFinally() throws Exception {
        var request = new MockHttpServletRequest();
        var state = new RequestLogState();
        request.setAttribute(RequestLogState.ATTRIBUTE, state);
        var previous = RequestContextHolder.getRequestAttributes();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try (var pool = Executors.newSingleThreadExecutor()) {
            var executor =
                    new CorrelatedTaskExecutor(new org.springframework.core.task.support.TaskExecutorAdapter(pool));
            var started = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            var handle = RequestCorrelationContext.capture();
            Future<?> running = executor.submit(() -> {
                assertThat(MDC.get("requestId")).isEqualTo(state.id);
                started.countDown();
                try {
                    assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
                assertThat(MDC.get("requestId")).isEqualTo(state.id);
            });
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            var cancelled = executor.submit(() -> {
                throw new AssertionError("cancelled task executed");
            });
            assertThat(cancelled.cancel(false)).isTrue();
            state.close();
            release.countDown();
            running.get(5, TimeUnit.SECONDS);
            assertThat(pool.submit(() -> MDC.get("requestId")).get(5, TimeUnit.SECONDS))
                    .isNull();
            assertThat(pool.submit(handle.wrap((Callable<String>) () -> MDC.get("requestId")))
                            .get(5, TimeUnit.SECONDS))
                    .isNull();
        } finally {
            RequestContextHolder.setRequestAttributes(previous);
        }
    }

    @Test
    void failingMdcProviderDoesNotReplaceBusinessResultOrException() throws Exception {
        var state = new RequestLogState();
        var request = new MockHttpServletRequest();
        request.setAttribute(RequestLogState.ATTRIBUTE, state);
        var previous = RequestContextHolder.getRequestAttributes();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        var handle = RequestCorrelationContext.capture();
        var original = new IllegalArgumentException("business failure");
        try (var mdc = mockStatic(MDC.class)) {
            mdc.when(() -> MDC.get("requestId")).thenThrow(new IllegalStateException("provider"));
            assertThat(handle.wrap((Callable<String>) () -> "original-result").call())
                    .isEqualTo("original-result");
            assertThatThrownBy(() -> handle.wrap((Callable<String>) () -> {
                                throw original;
                            })
                            .call())
                    .isSameAs(original);
        } finally {
            RequestContextHolder.setRequestAttributes(previous);
        }
        assertThat(state.close().outcome()).isEqualTo("UNKNOWN");
    }

    @Test
    void adviceAndFallbackShareOneSafeDiagnosticWithoutChangingErrorEnvelopes() throws Exception {
        var request = new MockHttpServletRequest();
        var state = new RequestLogState();
        request.setAttribute(RequestLogState.ATTRIBUTE, state);
        var writer = new ApiErrorResponseWriter(
                tools.jackson.databind.json.JsonMapper.builder().build());
        var handler = new ApiExceptionHandler(writer);
        try (var capture = new com.example.agenticorderservice.shared.logging.EventCapture(ApiExceptionHandler.class)) {
            handler.handle(new ApiInputException(ApiError.Code.INVALID_NAME), request, new MockHttpServletResponse());
            handler.handle(
                    new com.example.agenticorderservice.customer.application.CustomerNotFoundException(),
                    request,
                    new MockHttpServletResponse());
            assertThat(capture.messages()).isEmpty();
            var response = new MockHttpServletResponse();
            handler.handle(new IllegalStateException("password=secret-canary"), request, response);
            ExpectedApiErrors.assertResponse(
                    response.getStatus(), response.getContentAsString(), "INTERNAL_SERVER_ERROR");
            request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
            new ApiErrorController(writer).error(request, new MockHttpServletResponse());
            assertThat(capture.messages())
                    .containsExactly("event=HTTP_TECHNICAL_FAILURE category=INTERNAL_SERVER_ERROR");
        }
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "200,DEBUG",
        "204,DEBUG",
        "409,DEBUG",
        "200,INFO",
        "204,INFO",
        "409,INFO"
    })
    void completionLevelsPreserveStatusCorrelationAndMdcCleanup(int status, String configuredLevel) throws Exception {
        String method = status == 200 ? "GET" : "DELETE";
        String route = status == 200 ? "/customers" : "/customers/{id}";
        var request = new MockHttpServletRequest(method, "/customers/fixture");
        request.setAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, route);
        var response = new MockHttpServletResponse();
        var listener = new RequestLoggingListener();
        var event = new ServletRequestEvent(new MockServletContext(), request);
        listener.requestInitialized(event);
        String previousId = MDC.get("requestId");
        String previousOther = MDC.get("unrelated");
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(RequestLoggingListener.class);
        try (var capture =
                new com.example.agenticorderservice.shared.logging.EventCapture(RequestLoggingListener.class)) {
            logger.setLevel(ch.qos.logback.classic.Level.valueOf(configuredLevel));
            MDC.remove("requestId");
            MDC.put("unrelated", "preserved");
            new RequestCorrelationFilter().doFilter(request, response, (input, output) -> {
                assertThat(MDC.get("requestId")).isEqualTo(response.getHeader("X-Request-ID"));
                response.setStatus(status);
            });
            assertThat(MDC.get("requestId")).isNull();
            assertThat(capture.messages()).isEmpty();
            listener.requestDestroyed(event);
            listener.requestDestroyed(event);
            if (status == 409 && configuredLevel.equals("INFO")) {
                assertThat(logger.isDebugEnabled()).isFalse();
                assertThat(capture.events).isEmpty();
            } else {
                assertThat(capture.events).singleElement().satisfies(entry -> {
                    assertThat(entry.getFormattedMessage())
                            .startsWith("event=HTTP_REQUEST_COMPLETED method=" + method + " route=" + route + " status="
                                    + status + " outcome=COMPLETED ");
                    assertThat(entry.getLevel())
                            .isEqualTo(
                                    status == 409
                                            ? ch.qos.logback.classic.Level.DEBUG
                                            : ch.qos.logback.classic.Level.INFO);
                    assertThat(entry.getMDCPropertyMap())
                            .containsEntry("requestId", response.getHeader("X-Request-ID"));
                });
            }
            assertThat(response.getStatus()).isEqualTo(status);
            assertThat(UUID.fromString(response.getHeader("X-Request-ID")))
                    .hasToString(response.getHeader("X-Request-ID"));
            assertThat(MDC.get("requestId")).isNull();
            assertThat(MDC.get("unrelated")).isEqualTo("preserved");
        } finally {
            if (previousId == null) MDC.remove("requestId");
            else MDC.put("requestId", previousId);
            if (previousOther == null) MDC.remove("unrelated");
            else MDC.put("unrelated", previousOther);
        }
    }

    @Test
    void nestedErrorAndCommittedOutputPreserveMdcAndNeverEmitEarly() throws Exception {
        var request = new MockHttpServletRequest("GET", "/private-canary");
        var response = new MockHttpServletResponse();
        var listener = new RequestLoggingListener();
        var event = new ServletRequestEvent(new MockServletContext(), request);
        listener.requestInitialized(event);
        var state = (RequestLogState) request.getAttribute(RequestLogState.ATTRIBUTE);
        var filter = new RequestCorrelationFilter();
        try (var capture =
                new com.example.agenticorderservice.shared.logging.EventCapture(RequestLoggingListener.class)) {
            String old = MDC.get("requestId");
            filter.doFilter(request, response, (input, output) -> {
                assertThat(MDC.get("requestId")).isEqualTo(state.id);
                request.setDispatcherType(DispatcherType.ERROR);
                filter.doFilter(input, output, (nested, target) -> {
                    assertThat(MDC.get("requestId")).isEqualTo(state.id);
                    response.setStatus(500);
                    response.getWriter().write("original");
                    response.flushBuffer();
                });
                assertThat(MDC.get("requestId")).isEqualTo(state.id);
                assertThat(capture.messages()).isEmpty();
            });
            assertThat(MDC.get("requestId")).isEqualTo(old);
            listener.requestDestroyed(event);
            listener.requestDestroyed(event);
            assertThat(response.getContentAsString()).isEqualTo("original");
            assertThat(capture.messages()).singleElement().asString().contains("status=500");
            assertThat(MDC.get("requestId")).isEqualTo(old);
        }
    }

    @Test
    void outerFailureAfterNewerNestedStatusCannotRemainCompleted() {
        var state = new RequestLogState();
        long outer = state.enter("GET"), nested = state.enter("GET");
        state.observe(nested, 200, true, false, "/orders");
        state.observe(outer, 500, false, false, "/orders");
        var snapshot = state.close();
        assertThat(snapshot.status()).isZero();
        assertThat(snapshot.outcome()).isEqualTo("UNKNOWN");
        assertThat(snapshot.pending()).isEqualTo("ASYNC_ERROR");
        assertThat(state.close()).isNull();
    }

    @Test
    void concurrentFailureAndCompletionPreserveFailureBeforeTerminalClaim() throws Exception {
        var state = new RequestLogState();
        state.startCycle(0);
        var barrier = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var failure = pool.submit(() -> {
                barrier.await(5, TimeUnit.SECONDS);
                state.failure(false);
                return null;
            });
            var completion = pool.submit(() -> {
                barrier.await(5, TimeUnit.SECONDS);
                state.completed(1);
                return null;
            });
            failure.get(5, TimeUnit.SECONDS);
            completion.get(5, TimeUnit.SECONDS);
        }
        var snapshot = state.close();
        assertThat(snapshot.asyncCompleted()).isTrue();
        assertThat(snapshot.outcome()).isEqualTo("UNKNOWN");
        assertThat(snapshot.pending()).isEqualTo("ASYNC_ERROR");
    }

    @Test
    void competingStartCallbacksRegisterExactlyOneNextGeneration() throws Exception {
        var state = new RequestLogState();
        state.startCycle(0);
        var observer = new RequestCorrelationFilter.Observer(state, 1);
        var context = mock(AsyncContext.class);
        var barrier = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Void> call = () -> {
                barrier.await(5, TimeUnit.SECONDS);
                observer.onStartAsync(new AsyncEvent(context));
                return null;
            };
            var first = pool.submit(call);
            var second = pool.submit(call);
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        }
        verify(context).addListener(any());
        assertThat(state.cycle()).isEqualTo(2);
        observer.onComplete(new AsyncEvent(context)); // An old completion cannot finish the new cycle.
        assertThat(state.close().asyncCompleted()).isFalse();
    }

    @Test
    void everyExecutorForwardingMethodScopesOnlyOwnedMetadataAcrossReusedWorker() throws Exception {
        var previous = RequestContextHolder.getRequestAttributes();
        String unrelated = MDC.get("unrelated");
        try (var pool = Executors.newSingleThreadExecutor()) {
            var executor =
                    new CorrelatedTaskExecutor(new org.springframework.core.task.support.TaskExecutorAdapter(pool));
            MDC.put("unrelated", "parent-private");
            for (int i = 0; i < 2; i++) {
                var state = new RequestLogState();
                var request = new MockHttpServletRequest();
                request.setAttribute(RequestLogState.ATTRIBUTE, state);
                RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
                var observed = new java.util.concurrent.atomic.AtomicReference<String>();
                var done = new CountDownLatch(1);
                executor.execute(() -> {
                    observed.set(MDC.get("requestId"));
                    done.countDown();
                });
                assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
                assertThat(observed.get()).isEqualTo(state.id);
                executor.submitCompletable(() -> {
                            observed.set(MDC.get("requestId"));
                        })
                        .get(5, TimeUnit.SECONDS);
                assertThat(observed.get()).isEqualTo(state.id);
                assertThat(executor.submitCompletable(() -> MDC.get("requestId"))
                                .get(5, TimeUnit.SECONDS))
                        .isEqualTo(state.id);
                assertThat(executor.submit(() -> MDC.get("unrelated")).get(5, TimeUnit.SECONDS))
                        .isNull();
                var failure = new IllegalStateException("private");
                assertThatThrownBy(() -> executor.submitCompletable((Callable<String>) () -> {
                                    throw failure;
                                })
                                .get(5, TimeUnit.SECONDS))
                        .hasCause(failure);
                assertThat(pool.submit(() -> MDC.get("requestId")).get(5, TimeUnit.SECONDS))
                        .isNull();
            }
        } finally {
            RequestContextHolder.setRequestAttributes(previous);
            if (unrelated == null) MDC.remove("unrelated");
            else MDC.put("unrelated", unrelated);
        }
    }

    @Test
    void runningWorkerCancellationRestoresContextAfterActualWorkerExit() throws Exception {
        var previous = RequestContextHolder.getRequestAttributes();
        var state = new RequestLogState();
        var request = new MockHttpServletRequest();
        request.setAttribute(RequestLogState.ATTRIBUTE, state);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try (var pool = Executors.newSingleThreadExecutor()) {
            var executor =
                    new CorrelatedTaskExecutor(new org.springframework.core.task.support.TaskExecutorAdapter(pool));
            var entered = new CountDownLatch(1);
            var finished = new CountDownLatch(1);
            var workerId = new java.util.concurrent.atomic.AtomicReference<String>();
            var running = executor.submit(() -> {
                workerId.set(MDC.get("requestId"));
                entered.countDown();
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } finally {
                    finished.countDown();
                }
            });
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(running.cancel(true)).isTrue();
            assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(workerId.get()).isEqualTo(state.id);
            assertThat(pool.submit(() -> MDC.get("requestId")).get(5, TimeUnit.SECONDS))
                    .isNull();
        } finally {
            RequestContextHolder.setRequestAttributes(previous);
        }
    }

    @Test
    void httpTerminalAppenderFailureCannotChangeResponseOrRepeatEmissionAttempt() throws Exception {
        var request = new MockHttpServletRequest("GET", "/");
        var response = new MockHttpServletResponse();
        var listener = new RequestLoggingListener();
        var event = new ServletRequestEvent(new MockServletContext(), request);
        listener.requestInitialized(event);
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(RequestLoggingListener.class);
        var attempts = new java.util.concurrent.atomic.AtomicInteger();
        var appender = new ch.qos.logback.core.AppenderBase<ch.qos.logback.classic.spi.ILoggingEvent>() {
            @Override
            public void doAppend(ch.qos.logback.classic.spi.ILoggingEvent e) {
                attempts.incrementAndGet();
                throw new IllegalStateException("appender");
            }

            @Override
            protected void append(ch.qos.logback.classic.spi.ILoggingEvent e) {
                // doAppend deliberately fails before this hook can be reached.
            }
        };
        appender.start();
        logger.addAppender(appender);
        try {
            new RequestCorrelationFilter().doFilter(request, response, (input, output) -> {
                response.setStatus(202);
                response.getWriter().write("original");
            });
            listener.requestDestroyed(event);
            listener.requestDestroyed(event);
            assertThat(response.getStatus()).isEqualTo(202);
            assertThat(response.getContentAsString()).isEqualTo("original");
            assertThat(attempts.get()).isEqualTo(1);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void hostileHeadersAndBodyNeverEnterApplicationDiagnostics() throws Exception {
        var request = new MockHttpServletRequest("POST", "/sensitive-canary");
        request.setContent("password=body-canary".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        request.addHeader("X-Request-ID", "bad\r\ninjection-canary");
        request.addHeader("X-Request-ID", "x".repeat(1000));
        request.addHeader("Authorization", "Bearer secret-canary");
        request.addHeader("Cookie", "private-canary");
        var listener = new RequestLoggingListener();
        var event = new ServletRequestEvent(new MockServletContext(), request);
        listener.requestInitialized(event);
        var response = new MockHttpServletResponse();
        try (var capture =
                new com.example.agenticorderservice.shared.logging.EventCapture("com.example.agenticorderservice")) {
            new RequestCorrelationFilter().doFilter(request, response, (input, output) -> {
                response.setStatus(400);
            });
            listener.requestDestroyed(event);
            assertThat(response.getHeader("X-Request-ID")).matches("[0-9a-f-]{36}");
            assertThat(capture.messages())
                    .allSatisfy(m -> assertThat(m).doesNotContain("canary", "sensitive", "injection", "password="));
            assertThat(capture.events)
                    .allSatisfy(e -> assertThat(e.getThrowableProxy()).isNull());
        }
    }
}
