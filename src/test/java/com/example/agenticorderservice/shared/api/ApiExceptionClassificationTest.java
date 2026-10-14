package com.example.agenticorderservice.shared.api;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;

import static com.example.agenticorderservice.shared.api.ApiError.Code.*;
import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionClassificationTest {
    @Test
    void confirmsDuplicateEmailThroughStructuredHibernateMetadataAfterCompletion() {
        var violation = violation("23505", "uq_customers_email");
        var failure = new TransactionSystemException("completion failed",
                new DataIntegrityViolationException("flush failed", violation));

        assertThat(ApiExceptionHandler.classify(failure)).isEqualTo(DUPLICATE_EMAIL);
    }

    @Test
    void unrelatedConstraintsAndStatesRemainInternalFailures() {
        assertThat(ApiExceptionHandler.classify(violation("23505", "unrelated_unique")))
                .isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(ApiExceptionHandler.classify(violation("23514", "uq_customers_email")))
                .isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(ApiExceptionHandler.classify(violation("23505", null)))
                .isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(ApiExceptionHandler.classify(new SQLException("uq_customers_email", "23505")))
                .isEqualTo(INTERNAL_SERVER_ERROR);
    }

    @Test
    void followsNextSqlExceptionOnlyWhenEstablishmentIsProven() {
        var sql = new SQLException("wrapper", "HY000");
        sql.setNextException(new SQLException("connection failed", "08006"));

        assertThat(ApiExceptionHandler.classify(new CannotCreateTransactionException("acquisition", sql)))
                .isEqualTo(SERVICE_UNAVAILABLE);
        assertThat(ApiExceptionHandler.classify(new TransactionSystemException("uncertain completion", sql)))
                .isEqualTo(INTERNAL_SERVER_ERROR);
    }

    @Test
    void followsOriginalApplicationExceptionWithoutDependingOnWrapperMessages() {
        var failure = new TransactionSystemException("completion failed",
                new SQLException("unrelated failure", "HY000"));
        failure.initApplicationException(violation("23505", "uq_customers_email"));

        assertThat(ApiExceptionHandler.classify(failure)).isEqualTo(DUPLICATE_EMAIL);
    }

    @Test
    void connectionTypeWithoutEstablishmentEvidenceDoesNotBecomeServiceUnavailable() {
        var connection = new SQLTransientConnectionException("connection lost");

        assertThat(ApiExceptionHandler.classify(connection)).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(ApiExceptionHandler.classify(new CannotCreateTransactionException("acquisition", connection)))
                .isEqualTo(SERVICE_UNAVAILABLE);
    }

    @Test
    void cyclicCauseGraphsTerminateWithoutClassifyingMessages() {
        var first = new RuntimeException("23505 uq_customers_email");
        var second = new RuntimeException("connection failed", first);
        first.initCause(second);

        assertThat(ApiExceptionHandler.classify(first)).isEqualTo(INTERNAL_SERVER_ERROR);
    }

    private static ConstraintViolationException violation(String sqlState, String constraint) {
        return new ConstraintViolationException("private database details",
                new SQLException("private SQL", sqlState), constraint);
    }
}
