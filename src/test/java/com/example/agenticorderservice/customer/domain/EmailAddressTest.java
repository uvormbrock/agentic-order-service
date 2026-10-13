package com.example.agenticorderservice.customer.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

// PRD approved email policy and AC-011/AC-012: normalization, grammar and boundaries.
class EmailAddressTest {
    @Test void normalizesIdempotentlyAndPreservesLiteralPlusAndDots() {
        var email = new EmailAddress("\u2003 Alice+Orders@Example.COM \t");
        assertThat(email.value()).isEqualTo("alice+orders@example.com");
        assertThat(new EmailAddress(email.value())).isEqualTo(email);
        assertThat(email.hashCode()).isEqualTo(new EmailAddress("ALICE+ORDERS@example.com").hashCode());
        assertThat(new EmailAddress("a.b@example.com")).isNotEqualTo(new EmailAddress("ab@example.com"));
        assertThat(new EmailAddress("a+tag@example.com")).isNotEqualTo(new EmailAddress("a@example.com"));
    }
    @Test void acceptsApprovedPunctuationAndInternalHyphensWithoutExtraLengthRules() {
        new EmailAddress("!#$%&'*+-/=?^_`{|}~@ex--ample.com");
        new EmailAddress("a@x.y");
        assertThat(new EmailAddress("a".repeat(242) + "@example.com").value()).hasSize(254);
        assertThatIllegalArgumentException().isThrownBy(() -> new EmailAddress("a".repeat(243) + "@example.com"));
    }
    @Test void rejectsInvalidGrammarIncludingInternationalizedContentBeforeLowercase() {
        for (String invalid : new String[]{"", " ", ".a@example.com", "a.@example.com", "a..b@example.com",
                "a@@example.com", "a@example", "a@ex..com", "a@-ex.com", "a@ex-.com",
                "\"a\"@example.com", "á@example.com", "a@exämple.com", "\u212A@example.com", "a b@example.com"}) {
            assertThatIllegalArgumentException().as(invalid).isThrownBy(() -> new EmailAddress(invalid));
        }
        assertThatNullPointerException().isThrownBy(() -> new EmailAddress(null));
    }
}
