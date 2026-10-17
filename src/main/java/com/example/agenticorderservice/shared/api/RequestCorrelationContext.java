package com.example.agenticorderservice.shared.api;

import java.util.concurrent.Callable;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Explicit request-owned producer propagation; never captures Servlet or transaction resources. */
public final class RequestCorrelationContext {
    private static final String REQUEST_ID = "requestId";
    private final RequestLogState state;

    private RequestCorrelationContext(RequestLogState state) {
        this.state = state;
    }

    public static RequestCorrelationContext capture() {
        var attributes = RequestContextHolder.getRequestAttributes();
        var state = attributes instanceof ServletRequestAttributes servlet
                ? servlet.getRequest().getAttribute(RequestLogState.ATTRIBUTE)
                : null;
        return new RequestCorrelationContext(state instanceof RequestLogState owned ? owned : null);
    }

    public Runnable wrap(Runnable task) {
        return () -> {
            try (var scope = enter()) {
                task.run();
            }
        };
    }

    public <T> Callable<T> wrap(Callable<T> task) {
        return () -> {
            try (var scope = enter()) {
                return task.call();
            }
        };
    }

    private Scope enter() {
        // Closed handles cannot reactivate a request. Business work still retains its original behavior.
        return Scope.bind(state != null && state.open() ? state : null);
    }

    static final class Scope implements AutoCloseable {
        private String previous;
        private boolean bound;

        private Scope(RequestLogState state) {
            if (state == null) return;
            try {
                previous = MDC.get(REQUEST_ID);
                bound = true;
                MDC.put(REQUEST_ID, state.id);
            } catch (RuntimeException unavailable) {
                state.incomplete();
            }
        }

        static Scope bind(RequestLogState state) {
            return new Scope(state);
        }

        @Override
        public void close() {
            if (!bound) return;
            try {
                if (previous == null) MDC.remove(REQUEST_ID);
                else MDC.put(REQUEST_ID, previous);
            } catch (RuntimeException unavailable) {
                // An unavailable provider cannot be repaired by clearing unrelated context.
            }
        }
    }
}
