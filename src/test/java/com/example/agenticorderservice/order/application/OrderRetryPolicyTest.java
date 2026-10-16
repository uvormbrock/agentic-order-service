package com.example.agenticorderservice.order.application;

import java.sql.SQLException;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.transaction.TransactionSystemException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class OrderRetryPolicyTest {
    static SQLException serverFailure(String state, String routine) {
        return new PSQLException(new ServerErrorMessage("SERROR\0C" + state
                + "\0MUnrelated or localized server text\0R" + routine + "\0\0"));
    }

    @Test void requiresServerTimeoutDiagnosticAndOriginalLockCall() {
        var timeout = serverFailure("55P03", "ProcessInterrupts");
        assertThat(OrderRetryPolicy.transientFailure(new RuntimeException(timeout), true)).isTrue();
        assertThat(OrderRetryPolicy.transientFailure(new RuntimeException(timeout), false)).isFalse();
        assertThat(OrderRetryPolicy.transientFailure(new SQLException("canceling statement due to lock timeout", "55P03"), true)).isFalse();
        var noDiagnostics = org.mockito.Mockito.spy(new PSQLException("no diagnostics", org.postgresql.util.PSQLState.UNEXPECTED_ERROR));
        org.mockito.Mockito.when(noDiagnostics.getSQLState()).thenReturn("55P03");
        assertThat(OrderRetryPolicy.transientFailure(noDiagnostics, true)).isFalse();
    }

    @ParameterizedTest @ValueSource(strings={"LockAcquireExtended","exec_stmt_raise","","UnexpectedRoutine"})
    void rejectsNowaitUserRaisedAndMissingOrUnknownTimeoutOrigin(String routine) {
        assertThat(OrderRetryPolicy.transientFailure(serverFailure("55P03", routine), true)).isFalse();
    }

    @ParameterizedTest @NullAndEmptySource
    @ValueSource(strings={"HY000","23505","23514","23000","08006","08001","57014","57P01","55P03","40001"})
    void rejectsMixedNextExceptionStatesRegardlessOfOrder(String otherState) {
        var approved = new SQLException("deadlock", "40P01");
        approved.setNextException(new SQLException("other failure", otherState));
        assertThat(OrderRetryPolicy.transientFailure(approved, true)).isFalse();
        var other = new SQLException("other failure", otherState);
        other.setNextException(new SQLException("deadlock", "40P01"));
        assertThat(OrderRetryPolicy.transientFailure(other, true)).isFalse();
    }

    @ParameterizedTest @ValueSource(strings={"HY000","23514","08006","57014","40001"})
    void rejectsMixedCauseStatesEvenBehindWrappers(String state) {
        var nested = new SQLException("other failure", state);
        var outer = new SQLException("deadlock", "40P01", new IllegalStateException(nested));
        assertThat(OrderRetryPolicy.transientFailure(outer, true)).isFalse();
        var reverse = new SQLException("other failure", state, new RuntimeException(new SQLException("deadlock", "40P01")));
        assertThat(OrderRetryPolicy.transientFailure(reverse, true)).isFalse();
    }

    @ParameterizedTest @ValueSource(strings={"40P01","40001"})
    void acceptsConsistentKnownStatesIncludingSharedExceptionNodes(String state) {
        var shared = new SQLException("same database failure", state);
        var outer = new SQLException("wrapper", state, shared);
        outer.setNextException(shared);
        assertThat(OrderRetryPolicy.transientFailure(new IllegalStateException(outer), false)).isTrue();
    }

    @Test void rejectsMixedApprovedTimeoutAndDeadlockAndPlainTimeoutInChain() {
        var timeout = serverFailure("55P03", "ProcessInterrupts");
        timeout.setNextException(new SQLException("deadlock", "40P01"));
        assertThat(OrderRetryPolicy.transientFailure(timeout, true)).isFalse();
        var plain = new SQLException("no positive origin", "55P03");
        plain.setNextException(serverFailure("55P03", "ProcessInterrupts"));
        assertThat(OrderRetryPolicy.transientFailure(plain, true)).isFalse();
    }

    static Stream<Arguments> unsafeWrappers() {
        var sql = new SQLException("transient nested inside unsafe failure", "40P01");
        var business = new OrderFailure(OrderFailure.Reason.TECHNICAL, sql);
        var customer = new com.example.agenticorderservice.customer.application.CustomerNotFoundException();
        customer.initCause(sql);
        var product = new com.example.agenticorderservice.product.application.ProductNotFoundException();
        product.initCause(sql);
        var input = new com.example.agenticorderservice.shared.application.InputValidationException(
                com.example.agenticorderservice.shared.application.InputValidationException.FieldFailure.REQUIRED);
        input.initCause(sql);
        return Stream.of(business, customer, product, input,
                new jakarta.persistence.OptimisticLockException("conflict", sql),
                new org.springframework.dao.OptimisticLockingFailureException("conflict", sql),
                new org.springframework.dao.DataIntegrityViolationException("integrity", sql),
                new org.springframework.transaction.CannotCreateTransactionException("establishment", sql),
                new TransactionSystemException("completion", sql),
                new java.sql.SQLTransientConnectionException("contradictory connection type", "40P01"),
                new java.sql.SQLNonTransientConnectionException("contradictory connection type", "40P01"),
                new java.sql.SQLRecoverableException("connection recovery", "40P01"),
                new java.sql.SQLIntegrityConstraintViolationException("contradictory integrity type", "40P01"),
                new java.sql.SQLTimeoutException("unattributed timeout", "40P01"),
                new org.hibernate.exception.JDBCConnectionException("connection", sql),
                new org.springframework.dao.DataAccessResourceFailureException("connection", sql)).map(Arguments::of);
    }

    @ParameterizedTest @MethodSource("unsafeWrappers")
    void approvedNestedStateNeverOverridesUnsafeFailure(Throwable failure) {
        assertThat(OrderRetryPolicy.transientFailure(failure, true)).isFalse();
        var approvedFirst = new SQLException("transient state before unsafe failure", "40P01", failure);
        assertThat(OrderRetryPolicy.transientFailure(approvedFirst, true)).isFalse();
    }

    @Test void hiddenOriginalTransactionFailureAndSuppressedCleanupNeverAuthorizeRetry() {
        var transaction = new TransactionSystemException("completion", new SQLException("deadlock", "40P01"));
        transaction.initApplicationException(new SQLException("connection", "08006"));
        assertThat(OrderRetryPolicy.transientFailure(transaction, true)).isFalse();
        var sql = new SQLException("deadlock", "40P01");
        sql.addSuppressed(new IllegalStateException("resource cleanup uncertain"));
        assertThat(OrderRetryPolicy.transientFailure(sql, true)).isFalse();
    }

    @Test void cyclesTerminateAndFailClosedRatherThanTreatingRepeatedStateAsEvidence() {
        var first = new SQLException("one", "40P01");
        var second = new SQLException("two", "40P01");
        first.setNextException(second);
        second.setNextException(first);
        var causeOne = new IllegalStateException();
        var causeTwo = new IllegalStateException();
        causeOne.initCause(causeTwo);causeTwo.initCause(causeOne);
        var sqlWithCycle = new SQLException("deadlock", "40P01", causeOne);
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            assertThat(OrderRetryPolicy.transientFailure(first, true)).isFalse();
            assertThat(OrderRetryPolicy.transientFailure(sqlWithCycle, true)).isFalse();
        });
    }

    @Test void absentSqlEvidenceDoesNotAuthorizeRetry() {
        assertThat(OrderRetryPolicy.transientFailure(null, true)).isFalse();
        assertThat(OrderRetryPolicy.transientFailure(new IllegalStateException("no SQL evidence"), true)).isFalse();
    }
}
