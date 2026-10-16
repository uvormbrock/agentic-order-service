package com.example.agenticorderservice.shared.api;

import com.example.agenticorderservice.shared.logging.SafeEvent;
import jakarta.servlet.*;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public final class RequestLoggingListener implements ServletRequestListener {
    @Override
    public void requestInitialized(ServletRequestEvent event) {
        try {
            event.getServletRequest().setAttribute(RequestLogState.ATTRIBUTE, new RequestLogState());
        } catch (RuntimeException unavailable) {
            /* Preserve the original request lifecycle. */
        }
    }

    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        if (!(event.getServletRequest().getAttribute(RequestLogState.ATTRIBUTE) instanceof RequestLogState state))
            return;
        var snapshot = state.close();
        if (snapshot == null) return;
        try (var scope = RequestCorrelationContext.Scope.bind(state)) {
            var logger = LoggerFactory.getLogger(RequestLoggingListener.class);
            if (!snapshot.pending().equals("NONE"))
                SafeEvent.emit(
                        logger,
                        snapshot.pending().equals("ASYNC_ERROR") ? Level.ERROR : Level.WARN,
                        "event=HTTP_TECHNICAL_FAILURE category={}",
                        snapshot.pending());
            Level level = !snapshot.outcome().equals("COMPLETED") || snapshot.status() >= 500
                    ? Level.WARN
                    : snapshot.status() >= 400 ? Level.DEBUG : Level.INFO;
            SafeEvent.emit(
                    logger,
                    level,
                    "event=HTTP_REQUEST_COMPLETED method={} route={} status={} outcome={} durationMs={} cycles={} asyncCompleted={}",
                    snapshot.method(),
                    snapshot.route(),
                    snapshot.status() == 0 ? "UNKNOWN" : snapshot.status(),
                    snapshot.outcome(),
                    snapshot.durationMs(),
                    snapshot.cycles(),
                    snapshot.asyncCompleted());
        }
    }
}
