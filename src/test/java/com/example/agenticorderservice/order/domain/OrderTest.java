package com.example.agenticorderservice.order.domain;

import com.example.agenticorderservice.shared.domain.Money;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

// PRD AC-006/AC-007/AC-008/AC-015: consolidation, discount rounding and snapshots.
class OrderTest {
    private OrderLineInput line(UUID id, String price, int quantity) {
        return new OrderLineInput(id, "original", Money.of(price), quantity);
    }
    @Test void calculatesPrdThresholdAndHalfUpExamples() {
        String[][] examples = {{"99.99", "0", "99.99"}, {"100", "10", "90"}, {"100.05", "10.01", "90.04"}, {"150", "15", "135"}};
        for (String[] example : examples) {
            var order = new Order(UUID.randomUUID(), List.of(line(UUID.randomUUID(), example[0], 1)));
            assertThat(order.getDiscount()).isEqualTo(Money.of(example[1]));
            assertThat(order.getTotal()).isEqualTo(Money.of(example[2]));
        }
    }
    @Test void consolidatesAndProtectsOwnedItems() {
        UUID product = UUID.randomUUID();
        var order = new Order(UUID.randomUUID(), List.of(line(product, "25", 2), line(product, "25", 3)));
        assertThat(order.getItems()).hasSize(1);
        var item = order.getItems().getFirst();
        assertThat(item.getOrder()).isSameAs(order); assertThat(item.getId()).isNull();
        assertThat(item.getQuantity()).isEqualTo(5); assertThat(item.getLineTotal()).isEqualTo(Money.of("125"));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> order.getItems().clear());
    }
    @Test void rejectsEmptyInvalidAndExcessConsolidatedQuantities() {
        UUID product = UUID.randomUUID();
        assertThatIllegalArgumentException().isThrownBy(() -> new Order(UUID.randomUUID(), List.of()));
        for (int quantity : new int[]{0, -1, 10001, Integer.MAX_VALUE}) {
            assertThatIllegalArgumentException().isThrownBy(() -> line(product, "1", quantity));
        }
        new Order(UUID.randomUUID(), List.of(line(product, "1", 6000), line(product, "1", 4000)));
        assertThatIllegalArgumentException().isThrownBy(() -> new Order(UUID.randomUUID(),
                List.of(line(product, "1", 6000), line(product, "1", 4001))));
        assertThatNullPointerException().isThrownBy(() -> new Order(null, List.of(line(product, "1", 1))));
    }
    @Test void calculatesLargeMultiProductAmountsIndependently() {
        var order = new Order(UUID.randomUUID(), List.of(line(UUID.randomUUID(), "999999.99", 10000), line(UUID.randomUUID(), "999999.99", 10000)));
        assertThat(order.getItems().getFirst().getLineTotal()).isEqualTo(Money.of("9999999900.00"));
        assertThat(order.getSubtotal()).isEqualTo(Money.of("19999999800.00"));
        assertThat(order.getDiscount()).isEqualTo(Money.of("1999999980.00"));
        assertThat(order.getTotal()).isEqualTo(Money.of("17999999820.00"));
    }
}
