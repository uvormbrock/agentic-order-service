package com.example.agenticorderservice.order.application;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
class PlaceOrderCommandTest {
    @ParameterizedTest @ValueSource(ints={0,-1,10001,Integer.MAX_VALUE,Integer.MIN_VALUE})
    void rejectsInvalidQuantitiesWithoutArithmeticOverflow(int quantity) {
        assertThatExceptionOfType(OrderFailure.class).isThrownBy(() -> new PlaceOrderCommand(UUID.randomUUID(),List.of(new PlaceOrderCommand.Line(UUID.randomUUID(),quantity))))
                .satisfies(f -> assertThat(f.reason()).isEqualTo(OrderFailure.Reason.QUANTITY));
    }
    @Test void protectsRequiredFieldsEmptyItemsAndConsolidatedBoundary() {
        UUID id=UUID.randomUUID(); var valid=new PlaceOrderCommand.Line(id,1);
        assertThatThrownBy(() -> new PlaceOrderCommand(null,List.of(valid))).isInstanceOf(OrderFailure.class);
        assertThatThrownBy(() -> new PlaceOrderCommand(id,null)).isInstanceOf(OrderFailure.class);
        assertThatThrownBy(() -> new PlaceOrderCommand(id,List.of())).isInstanceOf(OrderFailure.class);
        assertThatThrownBy(() -> new PlaceOrderCommand(id,Arrays.asList((PlaceOrderCommand.Line)null))).isInstanceOf(OrderFailure.class);
        assertThatThrownBy(() -> new PlaceOrderCommand(id,List.of(new PlaceOrderCommand.Line(null,1)))).isInstanceOf(OrderFailure.class);
        assertThatThrownBy(() -> new PlaceOrderCommand(id,List.of(new PlaceOrderCommand.Line(id,null)))).isInstanceOf(OrderFailure.class);
        var lines=new ArrayList<>(List.of(new PlaceOrderCommand.Line(id,6000),new PlaceOrderCommand.Line(id,4000)));
        var command=new PlaceOrderCommand(id,lines);lines.clear(); assertThat(command.consolidated()).containsEntry(id,10000);
        assertThatThrownBy(() -> command.items().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> command.consolidated().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatExceptionOfType(OrderFailure.class).isThrownBy(() -> new PlaceOrderCommand(id,List.of(new PlaceOrderCommand.Line(id,6000),new PlaceOrderCommand.Line(id,4001))))
                .satisfies(f -> assertThat(f.reason()).isEqualTo(OrderFailure.Reason.QUANTITY));
    }
}
