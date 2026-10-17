package com.example.agenticorderservice.shared.domain;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

// Accepted D6: exact EUR values and scale-independent equality; PRD BR-009/BR-013.
class MoneyTest {
    @Test
    void canonicalizesAcceptedScaleAndHashWithoutRounding() {
        assertThat(Money.of("1")).isEqualTo(Money.of("1.0")).isEqualTo(Money.of("1.00"));
        assertThat(Money.of("1")).hasSameHashCodeAs(Money.of("1.00"));
        assertThat(Money.of("1").amount().scale()).isEqualTo(2);
    }

    @Test
    void rejectsExcessScaleNullAndOtherCurrencies() {
        for (String value : new String[] {"1.001", "10.000", "0.000"}) {
            assertThatIllegalArgumentException().isThrownBy(() -> Money.of(value));
        }
        assertThatNullPointerException().isThrownBy(() -> new Money(null));
        assertThatIllegalArgumentException().isThrownBy(() -> new Money(BigDecimal.ONE, Currency.getInstance("USD")));
        assertThatNullPointerException().isThrownBy(() -> new Money(BigDecimal.ONE, null));
    }

    @Test
    void performsExactSignedAndLargeArithmeticWithoutUniversalCap() {
        assertThat(Money.of("0.10").plus(Money.of("0.20"))).isEqualTo(Money.of("0.30"));
        assertThat(Money.of("1").minus(Money.of("2"))).isEqualTo(Money.of("-1"));
        assertThat(Money.of("999999.99").multiply(10000)).isEqualTo(Money.of("9999999900.00"));
        assertThat(Money.of("100000000000000000000").plus(Money.of("0.01")))
                .isEqualTo(Money.of("100000000000000000000.01"));
        assertThatNullPointerException().isThrownBy(() -> Money.zero().plus(null));
    }

    @ParameterizedTest
    @CsvSource({"-1,-1.00", "0,0.00", "1E+3,1000.00"})
    void preservesSignedZeroAndNegativeSourceScaleWithValueEquality(String source, String canonical) {
        var value = Money.of(source);
        assertThat(value.amount()).isEqualTo(new BigDecimal(canonical));
        assertThat(value).isEqualTo(Money.of(canonical));
        assertThat(value).hasSameHashCodeAs(Money.of(canonical));
        assertThat(value).isEqualByComparingTo(Money.of(canonical));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "invalid", "NaN", "Infinity", "1,00"})
    void rejectsInvalidDecimalInputs(String value) {
        assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> Money.of(value));
    }

    @Test
    void keepsArithmeticExactAndValuesImmutableAcrossAllOperations() {
        var source = Money.of("0.29");
        assertThat(source.multiply(3)).isEqualTo(Money.of("0.87"));
        assertThat(source.plus(Money.of("0.58"))).isEqualTo(Money.of("0.87"));
        assertThat(source.minus(Money.of("0.58"))).isEqualTo(Money.of("-0.29"));
        assertThat(source.multiply(-3)).isEqualTo(Money.of("-0.87"));
        assertThat(source).isEqualTo(Money.of("0.29"));
        assertThat(source).isLessThan(Money.of("0.30"));
        assertThat(source).isGreaterThan(Money.of("0.28"));
        assertThatNullPointerException().isThrownBy(() -> source.minus(null));
        assertThatNullPointerException().isThrownBy(() -> source.compareTo(null));
        assertThatNullPointerException().isThrownBy(() -> Money.of(null));
    }
}
