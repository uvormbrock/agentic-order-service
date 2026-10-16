package com.example.agenticorderservice.product.domain;

import static org.assertj.core.api.Assertions.*;

import com.example.agenticorderservice.shared.domain.Money;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// PRD AC-001/AC-011/AC-012: lifecycle and independent price/name boundaries.
class ProductTest {
    @Test
    void preservesUnicodeCaseAndCountsCodePoints() {
        var entity = new Product("  Älice😀  ", Money.of("1"));
        assertThat(entity.getName()).isEqualTo("Älice😀");
        assertThat(entity.getId()).isNull();
        assertThat(entity.isActive()).isTrue();
        new Product("😀".repeat(200), Money.of("1"));
        for (String invalid : new String[] {"", " ", "😀".repeat(201)}) {
            assertThatIllegalArgumentException().isThrownBy(() -> new Product(invalid, Money.of("1")));
        }
        assertThatNullPointerException().isThrownBy(() -> new Product(null, Money.of("1")));
    }

    @Test
    void keepsReferenceEqualityAndNeverReactivatesThroughUpdate() {
        var entity = new Product("a", Money.of("1"));
        var other = new Product("a", Money.of("1"));
        int hash = entity.hashCode();
        assertThat(entity).isNotEqualTo(other);
        entity.deactivate();
        entity.deactivate();
        entity.update("b", Money.of("1"));
        assertThat(entity.isActive()).isFalse();
        assertThat(entity.hashCode()).isEqualTo(hash);
        assertThat(entity.getName()).isEqualTo("b");
    }

    @Test
    void validatesProductRangeAndRetainsStateOnRejection() {
        new Product("free", Money.zero());
        new Product("maximum", Money.of("999999.99"));
        var product = new Product("original", Money.of("1"));
        for (String invalid : new String[] {"-0.01", "1000000"}) {
            assertThatIllegalArgumentException().isThrownBy(() -> product.update("changed", Money.of(invalid)));
            assertThat(product.getName()).isEqualTo("original");
        }
    }

    // Finding 4: BR-001/BR-011/BR-012. Failed updates retain name, price, identity and status.
    @ParameterizedTest(name = "{0}, inactive={2}")
    @MethodSource("rejectedNames")
    void rejectsNameUpdateWithoutChangingEitherFieldOrLifecycle(String scenario, String name, boolean inactive) {
        var product = new Product("Original", Money.of("25.00"));
        if (inactive) {
            product.deactivate();
        }
        var originalId = product.getId();
        assertThatThrownBy(() -> product.update(name, Money.of("40.00")))
                .isInstanceOf(name == null ? NullPointerException.class : IllegalArgumentException.class);
        assertThat(product.getName()).isEqualTo("Original");
        assertThat(product.getPrice()).isEqualTo(Money.of("25.00"));
        assertThat(product.isActive()).isEqualTo(!inactive);
        assertThat(product.getId()).isEqualTo(originalId);
    }

    @ParameterizedTest(name = "{0}, inactive={1}")
    @MethodSource("rejectedPrices")
    void rejectsPriceAfterValidNameWithoutPartialMutation(String price, boolean inactive) {
        var product = new Product("Original", Money.of("25.00"));
        if (inactive) {
            product.deactivate();
        }
        Money attemptedPrice = price == null ? null : Money.of(price);
        assertThatThrownBy(() -> product.update("Changed", attemptedPrice))
                .isInstanceOf(price == null ? NullPointerException.class : IllegalArgumentException.class);
        assertThat(product.getName()).isEqualTo("Original");
        assertThat(product.getPrice()).isEqualTo(Money.of("25.00"));
        assertThat(product.isActive()).isEqualTo(!inactive);
        assertThat(product.getId()).isNull();
    }

    static Stream<Arguments> rejectedPrices() {
        return Stream.of(false, true)
                .flatMap(inactive -> Stream.of(
                        Arguments.of(null, inactive),
                        Arguments.of("-0.01", inactive),
                        Arguments.of("1000000.00", inactive)));
    }

    @ParameterizedTest
    @MethodSource("activeStates")
    void replacesBothValidFieldsAndRetainsLifecycle(boolean inactive) {
        var product = new Product("Original", Money.of("25.00"));
        if (inactive) {
            product.deactivate();
        }
        product.update("\u2003 Äpfel😀 ", Money.of("40.05"));
        assertThat(product.getName()).isEqualTo("Äpfel😀");
        assertThat(product.getPrice()).isEqualTo(Money.of("40.05"));
        assertThat(product.isActive()).isEqualTo(!inactive);
        assertThat(product.getId()).isNull();
    }

    @Test
    void rejectsMissingPriceAndAcceptsMinimumNameWithoutLowercasing() {
        assertThatNullPointerException().isThrownBy(() -> new Product("Original", null));
        assertThat(new Product(" Ä ", Money.zero()).getName()).isEqualTo("Ä");
    }

    static Stream<Boolean> activeStates() {
        return Stream.of(false, true);
    }

    static Stream<Arguments> rejectedNames() {
        return Stream.of(false, true)
                .flatMap(inactive -> Stream.of(
                        Arguments.of("empty", "", inactive),
                        Arguments.of("whitespace", "\u2003\t ", inactive),
                        Arguments.of("201 code points", "😀".repeat(201), inactive),
                        Arguments.of("null", null, inactive),
                        Arguments.of("unpaired high surrogate", "a\uD800b", inactive),
                        Arguments.of("unpaired low surrogate", "a\uDC00b", inactive),
                        Arguments.of("upper low surrogate boundary", "a\uDFFFb", inactive)));
    }
}
