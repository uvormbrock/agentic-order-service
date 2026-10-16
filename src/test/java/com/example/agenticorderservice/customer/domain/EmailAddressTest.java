package com.example.agenticorderservice.customer.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// PRD approved email policy and AC-011/AC-012: normalization, grammar and boundaries.
class EmailAddressTest {
    @Test
    void normalizesIdempotentlyAndPreservesLiteralPlusAndDots() {
        var email = new EmailAddress("\u2003 Alice+Orders@Example.COM \t");
        assertThat(email.value()).isEqualTo("alice+orders@example.com");
        assertThat(new EmailAddress(email.value())).isEqualTo(email);
        assertThat(email.hashCode()).isEqualTo(new EmailAddress("ALICE+ORDERS@example.com").hashCode());
        assertThat(new EmailAddress("a.b@example.com")).isNotEqualTo(new EmailAddress("ab@example.com"));
        assertThat(new EmailAddress("a+tag@example.com")).isNotEqualTo(new EmailAddress("a@example.com"));
    }

    @Test
    void acceptsApprovedPunctuationAndInternalHyphensWithoutExtraLengthRules() {
        new EmailAddress("!#$%&'*+-/=?^_`{|}~@ex--ample.com");
        new EmailAddress("a@x.y");
        assertThat(new EmailAddress("a".repeat(242) + "@example.com").value()).hasSize(254);
        assertThatIllegalArgumentException().isThrownBy(() -> new EmailAddress("a".repeat(243) + "@example.com"));
    }

    @Test
    void rejectsInvalidGrammarIncludingInternationalizedContentBeforeLowercase() {
        for (String invalid : new String[] {
            "",
            " ",
            ".a@example.com",
            "a.@example.com",
            "a..b@example.com",
            "a@@example.com",
            "a@example",
            "a@ex..com",
            "a@-ex.com",
            "a@ex-.com",
            "\"a\"@example.com",
            "á@example.com",
            "a@exämple.com",
            "\u212A@example.com",
            "a b@example.com"
        }) {
            assertThatIllegalArgumentException().as(invalid).isThrownBy(() -> new EmailAddress(invalid));
        }
        assertThatNullPointerException().isThrownBy(() -> new EmailAddress(null));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "@example.com",
                "a@",
                "a@example.com.",
                "a@ex_.com",
                "a:b@example.com",
                "a@example.com extra",
                "a\uD800@example.com",
                "\u00A0a@example.com\u00A0"
            })
    void rejectsMissingPartsForbiddenSeparatorsAndNonStrippedInternationalContent(String value) {
        assertThatIllegalArgumentException().isThrownBy(() -> new EmailAddress(value));
    }

    @Test
    void appliesLengthAfterStripAndLowercaseWithoutUnapprovedLabelLimits() {
        String boundary = "A".repeat(242) + "@EXAMPLE.COM";
        var email = new EmailAddress("\u2003\t" + boundary + " \n");
        assertThat(email.value()).isEqualTo("a".repeat(242) + "@example.com");
        assertThat(email.value().codePointCount(0, email.value().length())).isEqualTo(254);
        assertThat(new EmailAddress("I@" + "a".repeat(100) + ".com").value())
                .isEqualTo("i@" + "a".repeat(100) + ".com");
    }
}
