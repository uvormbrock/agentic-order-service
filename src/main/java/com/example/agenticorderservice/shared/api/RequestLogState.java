package com.example.agenticorderservice.shared.api;

import java.util.Set;
import java.util.UUID;

/** Bounded metadata only. No request, response, exception or transaction is retained. */
final class RequestLogState {
    static final String ATTRIBUTE = RequestLogState.class.getName();
    final String id = UUID.randomUUID().toString();
    private final long started = System.nanoTime();
    private String method = "OTHER", route = "OTHER";
    private long sequence, failureSequence, statusSequence;
    private int cycle, status;
    private boolean closed, diagnostic, timeout, error, incomplete, asyncCompleted;

    synchronized long enter(String candidate) {
        if (closed) return -1;
        method = Set.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .contains(candidate)
                ? candidate
                : "OTHER";
        return ++sequence;
    }

    synchronized void observe(long token, int observed, boolean successful, boolean asyncStarted, String template) {
        if (closed) return;
        // A dispatch's entry token orders status candidates, never the occurrence of a failure.
        if (!successful) {
            failure(false);
            return;
        }
        if (token < statusSequence) return;
        if (Set.of("/products", "/products/{id}", "/customers", "/customers/{id}", "/orders", "/orders/{id}")
                .contains(template)) route = template;
        if (successful && !asyncStarted && token > failureSequence) {
            status = observed;
            statusSequence = token;
        }
    }

    synchronized int cycle() {
        return cycle;
    }

    synchronized boolean open() {
        return !closed;
    }

    synchronized int startCycle(int previous) {
        if (closed || cycle != previous) return 0;
        cycle++;
        asyncCompleted = false;
        status = 0;
        return cycle;
    }

    synchronized void completed(int generation) {
        if (!closed && generation == cycle) asyncCompleted = true;
    }

    synchronized void failure(boolean timedOut) {
        if (closed) return;
        timeout |= timedOut;
        error |= !timedOut;
        failureSequence = ++sequence;
        status = 0;
    }

    synchronized void incomplete() {
        if (!closed) incomplete = true;
    }

    synchronized boolean claimDiagnostic() {
        if (diagnostic || closed) return false;
        diagnostic = true;
        return true;
    }

    synchronized Snapshot close() {
        if (closed) return null;
        closed = true;
        String outcome = incomplete || status == 0
                ? "UNKNOWN"
                : timeout ? "COMPLETED_AFTER_TIMEOUT" : error ? "COMPLETED_AFTER_ERROR" : "COMPLETED";
        String pending = !diagnostic && (timeout || error) ? error ? "ASYNC_ERROR" : "TIMEOUT" : "NONE";
        diagnostic = true;
        return new Snapshot(
                id,
                method,
                route,
                incomplete ? 0 : status,
                outcome,
                pending,
                Math.max(0, System.nanoTime() - started) / 1_000_000,
                cycle,
                asyncCompleted);
    }

    record Snapshot(
            String id,
            String method,
            String route,
            int status,
            String outcome,
            String pending,
            long durationMs,
            int cycles,
            boolean asyncCompleted) {}
}
