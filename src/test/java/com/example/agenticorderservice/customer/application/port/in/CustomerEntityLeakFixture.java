package com.example.agenticorderservice.customer.application.port.in;

/** Deliberately invalid entity exposure outside the exact approved output port. */
public interface CustomerEntityLeakFixture {
    com.example.agenticorderservice.customer.domain.Customer get();
}
