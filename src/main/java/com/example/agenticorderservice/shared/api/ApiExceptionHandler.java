package com.example.agenticorderservice.shared.api;
import com.example.agenticorderservice.customer.application.CustomerNotFoundException;
import com.example.agenticorderservice.product.application.ProductNotFoundException;
import com.example.agenticorderservice.shared.application.InputValidationException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.bind.annotation.*;
import static com.example.agenticorderservice.shared.api.ApiError.Code.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    private final ApiErrorResponseWriter writer;
    public ApiExceptionHandler(ApiErrorResponseWriter writer) { this.writer = writer; }
    @ExceptionHandler(Exception.class)
    public void handle(Exception failure, HttpServletRequest request, HttpServletResponse response) throws IOException {
        ApiError.Code code = classify(failure);
        if (failure instanceof HttpRequestMethodNotSupportedException method) {
            String[] supported = method.getSupportedMethods();
            if (supported != null) {
                Set<String> allow = new TreeSet<>(Arrays.asList(supported));
                if (allow.contains("GET")) { allow.add("HEAD"); }
                allow.add("OPTIONS");
                response.setHeader("Allow", String.join(", ", allow));
            }
            if (request.getMethod().equals("DELETE") && request.getRequestURI().matches("/customers/[^/]+")) { code = OPERATION_NOT_AVAILABLE; }
        }
        writer.write(response, code);
    }
    public static ApiError.Code classify(Throwable failure) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Throwable> pending = new ArrayList<>(); pending.add(failure);
        boolean establishment = false;
        boolean connectionFailure = false;
        for (int index = 0; index < pending.size(); index++) {
            Throwable cause = pending.get(index);
            if (cause == null || !seen.add(cause)) { continue; }
            if (cause instanceof ApiInputException input) { return input.code(); }
            if (cause instanceof InputValidationException input) {
                return switch (input.failure()) {
                    case REQUIRED -> REQUIRED_FIELD_MISSING;
                    case NAME -> INVALID_NAME;
                    case PRICE -> INVALID_PRODUCT_PRICE;
                    case EMAIL -> INVALID_EMAIL;
                };
            }
            if (cause instanceof ProductNotFoundException) { return PRODUCT_NOT_FOUND; }
            if (cause instanceof CustomerNotFoundException) { return CUSTOMER_NOT_FOUND; }
            if (cause instanceof OptimisticLockException || cause instanceof OptimisticLockingFailureException) { return CONCURRENT_MODIFICATION; }
            if (cause instanceof HttpRequestMethodNotSupportedException) { return METHOD_NOT_ALLOWED; }
            if (cause instanceof HttpMediaTypeNotAcceptableException) { return NOT_ACCEPTABLE; }
            if (cause instanceof HttpMediaTypeNotSupportedException) { return UNSUPPORTED_MEDIA_TYPE; }
            if (cause instanceof NoResourceFoundException || cause instanceof NoHandlerFoundException) { return ENDPOINT_NOT_FOUND; }
            if (cause instanceof HttpMessageNotReadableException) {
                // Inspect nested typed decoder failures before using the generic syntax fallback.
                if (cause.getCause() == null) { return MALFORMED_JSON; }
            }
            if (cause instanceof CannotCreateTransactionException) { establishment = true; }
            if (cause instanceof org.hibernate.exception.JDBCConnectionException) { connectionFailure = true; }
            if (cause instanceof SQLException sql) {
                if (sql instanceof java.sql.SQLTransientConnectionException || sql instanceof java.sql.SQLNonTransientConnectionException || (sql.getSQLState() != null && sql.getSQLState().startsWith("08"))) { connectionFailure = true; }
                if ("23505".equals(sql.getSQLState()) && "uq_customers_email".equals(constraint(sql))) { return DUPLICATE_EMAIL; }
                if (sql.getNextException() != null) { pending.add(sql.getNextException()); }
            }
            if (cause instanceof TransactionSystemException transaction && transaction.getOriginalException() != cause) { pending.add(transaction.getOriginalException()); }
            if (cause.getCause() != null) { pending.add(cause.getCause()); }
        }
        return establishment && connectionFailure ? SERVICE_UNAVAILABLE : failure instanceof HttpMessageNotReadableException ? MALFORMED_JSON : INTERNAL_SERVER_ERROR;
    }
    private static String constraint(SQLException failure) {
        if (!failure.getClass().getName().equals("org.postgresql.util.PSQLException")) { return null; }
        try {
            Object detail = failure.getClass().getMethod("getServerErrorMessage").invoke(failure);
            return detail == null ? null : (String) detail.getClass().getMethod("getConstraint").invoke(detail);
        } catch (ReflectiveOperationException unavailable) { return null; }
    }
}
