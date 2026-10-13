package com.example.agenticorderservice.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {
    private static final Currency EUR = Currency.getInstance("EUR");

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (!EUR.equals(currency) || amount.scale() > 2) {
            throw new IllegalArgumentException("Money requires EUR and at most two source decimal places");
        }
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    public Money(BigDecimal amount) {
        this(amount, EUR);
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public static Money zero() {
        return of("0");
    }

    public Money plus(Money other) {
        compatible(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        compatible(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money multiply(int quantity) {
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
    }
    @Override
    public int compareTo(Money other) {
        compatible(other);
        return amount.compareTo(other.amount);
    }

    private void compatible(Money other) {
        Objects.requireNonNull(other, "other");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Incompatible currencies");
        }
    }
}
