package com.example.agenticorderservice.customer.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

// PRD customer lifecycle and AC-011/AC-012: controlled updates and name limits.
class CustomerTest {
    @Test void preservesUnicodeCaseAndCountsCodePoints() {
        var entity = new Customer("  Älice😀  ", new EmailAddress("a@example.com"));
        assertThat(entity.getName()).isEqualTo("Älice😀");
        assertThat(entity.getId()).isNull(); assertThat(entity.isActive()).isTrue();
        new Customer("😀".repeat(200), new EmailAddress("a@example.com"));
        for (String invalid : new String[]{"", " ", "😀".repeat(201)}) {
            assertThatIllegalArgumentException().isThrownBy(() -> new Customer(invalid, new EmailAddress("a@example.com")));
        }
        assertThatNullPointerException().isThrownBy(() -> new Customer(null, new EmailAddress("a@example.com")));
    }
    @Test void keepsReferenceEqualityAndNeverReactivatesThroughUpdate() {
        var entity = new Customer("a", new EmailAddress("a@example.com"));
        var other = new Customer("a", new EmailAddress("a@example.com"));
        int hash = entity.hashCode();
        assertThat(entity).isNotEqualTo(other);
        entity.deactivate(); entity.deactivate(); entity.update("b", new EmailAddress("a@example.com"));
        assertThat(entity.isActive()).isFalse(); assertThat(entity.hashCode()).isEqualTo(hash);
        assertThat(entity.getName()).isEqualTo("b");
    }
}
