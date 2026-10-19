package com.example.agenticorderservice.customer.application;

public final class CustomerHasOrdersException extends RuntimeException {
    public CustomerHasOrdersException() {
        super("Customer has referencing orders");
    }
}
