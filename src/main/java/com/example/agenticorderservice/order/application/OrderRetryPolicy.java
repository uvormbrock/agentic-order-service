package com.example.agenticorderservice.order.application;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Map;

final class OrderRetryPolicy {
    private OrderRetryPolicy() { }

    private record Visit(Throwable failure, boolean completed) { }

    static boolean transientFailure(Throwable failure, boolean originalLockCallFailed) {
        if (failure == null) { return false; }
        // FALSE means an active DFS path, TRUE a completed node: shared causes are not cycles.
        Map<Throwable, Boolean> visited = new IdentityHashMap<>();
        var pending = new ArrayDeque<Visit>();
        pending.push(new Visit(failure, false));
        String approvedState = null;
        while (!pending.isEmpty()) {
            Visit visit = pending.pop();
            Throwable cause = visit.failure();
            if (visit.completed()) {
                visited.put(cause, true);
                continue;
            }
            Boolean completed = visited.get(cause);
            if (Boolean.FALSE.equals(completed)) { return false; }
            if (Boolean.TRUE.equals(completed)) { continue; }
            visited.put(cause, false);
            if (unsafeFailure(cause) || cause.getSuppressed().length != 0) { return false; }
            if (cause instanceof SQLException sql) {
                String state = sql.getSQLState();
                boolean approved = "40P01".equals(state) || "40001".equals(state)
                        || (originalLockCallFailed && "55P03".equals(state) && postgresLockTimeout(sql));
                // Every SQL exception must agree. Unknown or mixed states are not retry evidence.
                if (!approved || (approvedState != null && !approvedState.equals(state))) { return false; }
                approvedState = state;
            }
            pending.push(new Visit(cause, true));
            if (cause instanceof SQLException sql && sql.getNextException() != null) {
                pending.push(new Visit(sql.getNextException(), false));
            }
            if (cause.getCause() != null) { pending.push(new Visit(cause.getCause(), false)); }
        }
        return approvedState != null;
    }

    private static boolean unsafeFailure(Throwable cause) {
        return cause instanceof java.sql.SQLTransientConnectionException
                || cause instanceof java.sql.SQLNonTransientConnectionException
                || cause instanceof java.sql.SQLRecoverableException
                || cause instanceof java.sql.SQLIntegrityConstraintViolationException
                || cause instanceof java.sql.SQLTimeoutException
                || cause instanceof org.hibernate.exception.JDBCConnectionException
                || cause instanceof org.springframework.dao.DataAccessResourceFailureException
                || cause instanceof OrderFailure
                || cause instanceof com.example.agenticorderservice.customer.application.CustomerNotFoundException
                || cause instanceof com.example.agenticorderservice.product.application.ProductNotFoundException
                || cause instanceof com.example.agenticorderservice.shared.application.InputValidationException
                || cause instanceof jakarta.persistence.OptimisticLockException
                || cause instanceof org.springframework.dao.OptimisticLockingFailureException
                || cause instanceof org.springframework.dao.DataIntegrityViolationException
                || cause instanceof org.springframework.transaction.TransactionSystemException
                || cause instanceof org.springframework.transaction.CannotCreateTransactionException;
    }

    private static boolean postgresLockTimeout(SQLException sql) {
        if (!sql.getClass().getName().equals("org.postgresql.util.PSQLException")) { return false; }
        try {
            // The driver is runtime-scoped. Read its public structured diagnostics without
            // changing Maven scope or parsing localized exception messages. PostgreSQL 17
            // emits 55P03 from ProcessInterrupts for lock_timeout, unlike NOWAIT/RAISE.
            Object server = sql.getClass().getMethod("getServerErrorMessage").invoke(sql);
            return server != null && "ProcessInterrupts".equals(server.getClass().getMethod("getRoutine").invoke(server));
        } catch (ReflectiveOperationException | SecurityException unavailable) {
            // Missing/changed diagnostic APIs cannot positively establish timeout origin.
            return false;
        }
    }
}
