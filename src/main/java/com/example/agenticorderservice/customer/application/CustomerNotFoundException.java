package com.example.agenticorderservice.customer.application;
public final class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException() { super("Customer not found"); }
}
