package com.example.agenticorderservice.shared.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Currency;
import static org.assertj.core.api.Assertions.*;

// Accepted D6: exact EUR values and scale-independent equality; PRD BR-009/BR-013.
class MoneyTest {
    @Test void canonicalizesAcceptedScaleAndHashWithoutRounding() {
        assertThat(Money.of("1")).isEqualTo(Money.of("1.0")).isEqualTo(Money.of("1.00"));
        assertThat(Money.of("1").hashCode()).isEqualTo(Money.of("1.00").hashCode());
        assertThat(Money.of("1").amount().scale()).isEqualTo(2);
    }
    @Test void rejectsExcessScaleNullAndOtherCurrencies() {
        for (String value : new String[]{"1.001", "10.000", "0.000"}) {
            assertThatIllegalArgumentException().isThrownBy(() -> Money.of(value));
        }
        assertThatNullPointerException().isThrownBy(() -> new Money(null));
        assertThatIllegalArgumentException().isThrownBy(() -> new Money(BigDecimal.ONE, Currency.getInstance("USD")));
        assertThatNullPointerException().isThrownBy(() -> new Money(BigDecimal.ONE, null));
    }
    @Test void performsExactSignedAndLargeArithmeticWithoutUniversalCap() {
        assertThat(Money.of("0.10").plus(Money.of("0.20"))).isEqualTo(Money.of("0.30"));
        assertThat(Money.of("1").minus(Money.of("2"))).isEqualTo(Money.of("-1"));
        assertThat(Money.of("999999.99").multiply(10000)).isEqualTo(Money.of("9999999900.00"));
        assertThat(Money.of("100000000000000000000").plus(Money.of("0.01")))
                .isEqualTo(Money.of("100000000000000000000.01"));
        assertThatNullPointerException().isThrownBy(() -> Money.zero().plus(null));
    }
}
