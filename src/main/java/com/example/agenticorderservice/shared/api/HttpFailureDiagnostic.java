package com.example.agenticorderservice.shared.api;

import com.example.agenticorderservice.order.application.OrderFailure;
import com.example.agenticorderservice.shared.logging.SafeEvent;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

final class HttpFailureDiagnostic {
    private HttpFailureDiagnostic() {}

    static void record(HttpServletRequest request, ApiError.Code code, Throwable failure) {
        if (code.status() < 500) return;
        RequestLogState state =
                request.getAttribute(RequestLogState.ATTRIBUTE) instanceof RequestLogState owned ? owned : null;
        if (state != null && !state.claimDiagnostic()) return;
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = failure; cause != null && seen.add(cause); cause = cause.getCause()) {
            if (cause instanceof OrderFailure order && order.diagnosticOwned()) return;
        }
        SafeEvent.emit(
                LoggerFactory.getLogger(ApiExceptionHandler.class),
                Level.ERROR,
                "event=HTTP_TECHNICAL_FAILURE category={}",
                code.name());
    }
}
