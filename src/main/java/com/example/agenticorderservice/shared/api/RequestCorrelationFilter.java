package com.example.agenticorderservice.shared.api;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.web.servlet.HandlerMapping;

/** Per-dispatch observation only; the request listener exclusively owns terminal emission. */
public final class RequestCorrelationFilter implements Filter {
    @Override
    public void doFilter(ServletRequest input, ServletResponse output, FilterChain chain)
            throws IOException, ServletException {
        if (!(input instanceof HttpServletRequest request)
                || !(output instanceof HttpServletResponse response)
                || !(request.getAttribute(RequestLogState.ATTRIBUTE) instanceof RequestLogState state)) {
            chain.doFilter(input, output);
            return;
        }
        long token;
        try {
            token = state.enter(request.getMethod());
        } catch (RuntimeException unavailable) {
            state.incomplete();
            token = -1;
        }
        boolean successful = false;
        try (var scope = RequestCorrelationContext.Scope.bind(state)) {
            header(response, state);
            try {
                chain.doFilter(new ObservedRequest(request, state), response);
                successful = true;
            } finally {
                try {
                    header(response, state);
                    Object template = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
                    state.observe(
                            token,
                            response.getStatus(),
                            successful,
                            request.isAsyncStarted(),
                            template instanceof String text ? text : "OTHER");
                } catch (RuntimeException unavailable) {
                    state.incomplete();
                }
            }
        }
    }

    private static void header(HttpServletResponse response, RequestLogState state) {
        try {
            if (!response.isCommitted()) response.setHeader("X-Request-ID", state.id);
        } catch (RuntimeException unavailable) {
            state.incomplete();
        }
    }

    static final class ObservedRequest extends HttpServletRequestWrapper {
        private final RequestLogState state;

        ObservedRequest(HttpServletRequest request, RequestLogState state) {
            super(request);
            this.state = state;
        }

        @Override
        public AsyncContext startAsync() {
            int previous = state.cycle();
            AsyncContext context = super.startAsync();
            attach(context, previous);
            return context;
        }

        @Override
        public AsyncContext startAsync(ServletRequest request, ServletResponse response) {
            int previous = state.cycle();
            AsyncContext context = super.startAsync(request, response);
            attach(context, previous);
            return context;
        }

        private void attach(AsyncContext context, int previous) {
            try {
                int generation = state.startCycle(previous);
                if (generation != 0) context.addListener(new Observer(state, generation));
            } catch (RuntimeException unavailable) {
                state.incomplete();
            }
        }
    }

    static final class Observer implements AsyncListener {
        private final RequestLogState state;

        private final int generation;

        Observer(RequestLogState state, int generation) {
            this.state = state;
            this.generation = generation;
        }

        @Override
        public void onComplete(AsyncEvent event) {
            // Completion is an observation; no completed response is inspected.
            try (var scope = RequestCorrelationContext.Scope.bind(state)) {
                state.completed(generation);
            }
        }

        @Override
        public void onTimeout(AsyncEvent event) {
            try (var scope = RequestCorrelationContext.Scope.bind(state)) {
                state.failure(true);
            }
        }

        @Override
        public void onError(AsyncEvent event) {
            try (var scope = RequestCorrelationContext.Scope.bind(state)) {
                state.failure(false);
            }
        }

        @Override
        public void onStartAsync(AsyncEvent event) {
            try (var scope = RequestCorrelationContext.Scope.bind(state)) {
                int next = state.startCycle(generation);
                if (next != 0) event.getAsyncContext().addListener(new Observer(state, next));
            } catch (RuntimeException unavailable) {
                state.incomplete();
            }
        }
    }
}
