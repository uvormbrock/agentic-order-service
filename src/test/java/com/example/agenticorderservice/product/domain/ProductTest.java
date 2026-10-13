package com.example.agenticorderservice.product.domain;

import com.example.agenticorderservice.shared.domain.Money;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

// PRD AC-001/AC-011/AC-012: lifecycle and independent price/name boundaries.
class ProductTest {
    @Test void preservesUnicodeCaseAndCountsCodePoints() {
        var entity = new Product("  Älice😀  ", Money.of("1"));
        assertThat(entity.getName()).isEqualTo("Älice😀");
        assertThat(entity.getId()).isNull(); assertThat(entity.isActive()).isTrue();
        new Product("😀".repeat(200), Money.of("1"));
        for (String invalid : new String[]{"", " ", "😀".repeat(201)}) {
            assertThatIllegalArgumentException().isThrownBy(() -> new Product(invalid, Money.of("1")));
        }
        assertThatNullPointerException().isThrownBy(() -> new Product(null, Money.of("1")));
    }
    @Test void keepsReferenceEqualityAndNeverReactivatesThroughUpdate() {
        var entity = new Product("a", Money.of("1"));
        var other = new Product("a", Money.of("1"));
        int hash = entity.hashCode();
        assertThat(entity).isNotEqualTo(other);
        entity.deactivate(); entity.deactivate(); entity.update("b", Money.of("1"));
        assertThat(entity.isActive()).isFalse(); assertThat(entity.hashCode()).isEqualTo(hash);
        assertThat(entity.getName()).isEqualTo("b");
    }
    @Test void validatesProductRangeAndRetainsStateOnRejection() {
        new Product("free", Money.zero()); new Product("maximum", Money.of("999999.99"));
        var product = new Product("original", Money.of("1"));
        for (String invalid : new String[]{"-0.01", "1000000"}) {
            assertThatIllegalArgumentException().isThrownBy(() -> product.update("changed", Money.of(invalid)));
            assertThat(product.getName()).isEqualTo("original");
        }
    }
}
