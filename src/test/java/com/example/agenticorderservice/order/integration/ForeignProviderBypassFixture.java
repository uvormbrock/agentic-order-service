package com.example.agenticorderservice.order.integration;

/** Negative fixture: a bridge must depend on CustomerFacts instead of its implementation. */
public class ForeignProviderBypassFixture {
    com.example.agenticorderservice.customer.application.CustomerOrderAccess concrete;
}
