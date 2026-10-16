package com.example.agenticorderservice.shared.logging;

import ch.qos.logback.classic.*;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.slf4j.LoggerFactory;

/** Test-owned appender and logger level, always restored by the owning test. */
public final class EventCapture implements AutoCloseable {
    public final List<ILoggingEvent> events = new CopyOnWriteArrayList<>();
    private final BlockingQueue<ILoggingEvent> arrivals = new LinkedBlockingQueue<>();
    private final Logger logger;
    private final Level previous;
    private final AppenderBase<ILoggingEvent> appender = new AppenderBase<>() {
        @Override
        protected void append(ILoggingEvent event) {
            event.prepareForDeferredProcessing();
            events.add(event);
            arrivals.add(event);
        }
    };

    public EventCapture(Class<?> owner) {
        this(owner.getName());
    }

    public EventCapture(String owner) {
        logger = (Logger) LoggerFactory.getLogger(owner);
        previous = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender.start();
        logger.addAppender(appender);
    }

    public List<String> messages() {
        return events.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    public ILoggingEvent await(String requestId, String event) throws InterruptedException {
        long end = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < end) {
            for (var entry : events) {
                if (Objects.equals(requestId, entry.getMDCPropertyMap().get("requestId"))
                        && entry.getFormattedMessage().startsWith("event=" + event + " ")) return entry;
            }
            arrivals.poll(Math.max(1, end - System.nanoTime()), TimeUnit.NANOSECONDS);
        }
        throw new AssertionError("Missing request-owned event: " + event);
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(previous);
    }
}
