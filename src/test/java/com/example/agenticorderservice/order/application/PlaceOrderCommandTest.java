package com.example.agenticorderservice.order.application;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PlaceOrderCommandTest {
    @ParameterizedTest
    @ValueSource(ints = {0, -1, 10001, Integer.MAX_VALUE, Integer.MIN_VALUE})
    void rejectsInvalidQuantitiesWithoutArithmeticOverflow(int quantity) {
        UUID customer = UUID.randomUUID();
        var items = List.of(new PlaceOrderCommand.Line(UUID.randomUUID(), quantity));
        assertThatExceptionOfType(OrderFailure.class)
                .isThrownBy(() -> new PlaceOrderCommand(customer, items))
                .satisfies(f -> assertThat(f.reason()).isEqualTo(OrderFailure.Reason.QUANTITY));
    }

    @Test
    void protectsRequiredFieldsEmptyItemsAndConsolidatedBoundary() {
        UUID id = UUID.randomUUID();
        var valid = new PlaceOrderCommand.Line(id, 1);
        var validItems = List.of(valid);
        List<PlaceOrderCommand.Line> emptyItems = List.of();
        var nullItem = Arrays.asList((PlaceOrderCommand.Line) null);
        var missingProduct = List.of(new PlaceOrderCommand.Line(null, 1));
        var missingQuantity = List.of(new PlaceOrderCommand.Line(id, null));
        assertThatThrownBy(() -> new PlaceOrderCommand(null, validItems)).isInstanceOf(OrderFailure.class);
        assertThatThrownBy(() -> new PlaceOrderCommand(id, null)).isInstanceOf(OrderFailure.class);
        assertThatThrownBy(() -> new PlaceOrderCommand(id, emptyItems)).isInstanceOf(OrderFailure.class);
        assertThatThrownBy(() -> new PlaceOrderCommand(id, nullItem)).isInstanceOf(OrderFailure.class);
        assertThatThrownBy(() -> new PlaceOrderCommand(id, missingProduct)).isInstanceOf(OrderFailure.class);
        assertThatThrownBy(() -> new PlaceOrderCommand(id, missingQuantity)).isInstanceOf(OrderFailure.class);
        var lines =
                new ArrayList<>(List.of(new PlaceOrderCommand.Line(id, 6000), new PlaceOrderCommand.Line(id, 4000)));
        var command = new PlaceOrderCommand(id, lines);
        lines.clear();
        assertThat(command.consolidated()).containsEntry(id, 10000);
        var immutableItems = command.items();
        var immutableQuantities = command.consolidated();
        assertThatThrownBy(immutableItems::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(immutableQuantities::clear).isInstanceOf(UnsupportedOperationException.class);
        var excessiveItems = List.of(new PlaceOrderCommand.Line(id, 6000), new PlaceOrderCommand.Line(id, 4001));
        assertThatExceptionOfType(OrderFailure.class)
                .isThrownBy(() -> new PlaceOrderCommand(id, excessiveItems))
                .satisfies(f -> assertThat(f.reason()).isEqualTo(OrderFailure.Reason.QUANTITY));
    }
}
