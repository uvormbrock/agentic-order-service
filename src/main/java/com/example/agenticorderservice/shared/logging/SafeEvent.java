package com.example.agenticorderservice.shared.logging;

import org.slf4j.Logger;
import org.slf4j.event.Level;

/** A narrow best-effort emission boundary; event arguments are owner-allowlisted primitives. */
public final class SafeEvent {
    private SafeEvent() {}

    public static void emit(Logger logger, Level level, String template, Object... fields) {
        try {
            logger.atLevel(level).log(template, fields);
        } catch (RuntimeException unavailable) {
            // Logging is not a business operation and must never cause replay or mask a result.
        }
    }
}
