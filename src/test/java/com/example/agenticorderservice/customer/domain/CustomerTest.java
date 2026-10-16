package com.example.agenticorderservice.customer.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// PRD customer lifecycle and AC-011/AC-012: controlled updates and name limits.
class CustomerTest {
    @Test
    void preservesUnicodeCaseAndCountsCodePoints() {
        var entity = new Customer("  Älice😀  ", new EmailAddress("a@example.com"));
        assertThat(entity.getName()).isEqualTo("Älice😀");
        assertThat(entity.getId()).isNull();
        assertThat(entity.isActive()).isTrue();
        new Customer("😀".repeat(200), new EmailAddress("a@example.com"));
        for (String invalid : new String[] {"", " ", "😀".repeat(201)}) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new Customer(invalid, new EmailAddress("a@example.com")));
        }
        assertThatNullPointerException().isThrownBy(() -> new Customer(null, new EmailAddress("a@example.com")));
    }

    @Test
    void keepsReferenceEqualityAndNeverReactivatesThroughUpdate() {
        var entity = new Customer("a", new EmailAddress("a@example.com"));
        var other = new Customer("a", new EmailAddress("a@example.com"));
        int hash = entity.hashCode();
        assertThat(entity).isNotEqualTo(other);
        entity.deactivate();
        entity.deactivate();
        entity.update("b", new EmailAddress("a@example.com"));
        assertThat(entity.isActive()).isFalse();
        assertThat(entity.hashCode()).isEqualTo(hash);
        assertThat(entity.getName()).isEqualTo("b");
    }

    // Finding 4: BR-011/BR-012. Validate all fields before changing any state.
    @ParameterizedTest(name = "{0}, inactive={2}")
    @MethodSource("rejectedNames")
    void rejectsNameUpdateWithoutChangingEitherFieldOrLifecycle(String scenario, String name, boolean inactive) {
        var originalEmail = new EmailAddress("original@example.com");
        var customer = new Customer("Original", originalEmail);
        if (inactive) {
            customer.deactivate();
        }
        var originalId = customer.getId();
        assertThatThrownBy(() -> customer.update(name, new EmailAddress("changed@example.com")))
                .isInstanceOf(name == null ? NullPointerException.class : IllegalArgumentException.class);
        assertThat(customer.getName()).isEqualTo("Original");
        assertThat(customer.getEmail()).isEqualTo(originalEmail);
        assertThat(customer.isActive()).isEqualTo(!inactive);
        assertThat(customer.getId()).isEqualTo(originalId);
    }

    @ParameterizedTest
    @MethodSource("activeStates")
    void rejectsNullEmailAfterValidNameWithoutPartialMutation(boolean inactive) {
        var email = new EmailAddress("original@example.com");
        var customer = new Customer("Original", email);
        if (inactive) {
            customer.deactivate();
        }
        assertThatNullPointerException().isThrownBy(() -> customer.update("Changed", null));
        assertThat(customer.getName()).isEqualTo("Original");
        assertThat(customer.getEmail()).isEqualTo(email);
        assertThat(customer.isActive()).isEqualTo(!inactive);
        assertThat(customer.getId()).isNull();
    }

    @ParameterizedTest
    @MethodSource("activeStates")
    void replacesBothValidFieldsAndRetainsLifecycle(boolean inactive) {
        var customer = new Customer("Original", new EmailAddress("original@example.com"));
        if (inactive) {
            customer.deactivate();
        }
        customer.update("\u2003 Älice😀 ", new EmailAddress(" CHANGED@Example.COM "));
        assertThat(customer.getName()).isEqualTo("Älice😀");
        assertThat(customer.getEmail()).isEqualTo(new EmailAddress("changed@example.com"));
        assertThat(customer.isActive()).isEqualTo(!inactive);
        assertThat(customer.getId()).isNull();
    }

    @Test
    void rejectsMissingEmailAndAcceptsMinimumNameWithoutLowercasing() {
        assertThatNullPointerException().isThrownBy(() -> new Customer("Original", null));
        assertThat(new Customer(" Ä ", new EmailAddress("a@example.com")).getName())
                .isEqualTo("Ä");
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
