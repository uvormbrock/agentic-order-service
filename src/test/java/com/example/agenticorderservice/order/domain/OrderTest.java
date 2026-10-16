package com.example.agenticorderservice.order.domain;

import static org.assertj.core.api.Assertions.*;

import com.example.agenticorderservice.product.domain.Product;
import com.example.agenticorderservice.shared.domain.Money;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

// PRD AC-006/AC-007/AC-008/AC-015: consolidation, discount rounding and snapshots.
class OrderTest {
    private OrderLineInput line(UUID id, String price, int quantity) {
        return new OrderLineInput(id, "original", Money.of(price), quantity);
    }

    @Test
    void calculatesPrdThresholdAndHalfUpExamples() {
        String[][] examples = {
            {"99.99", "0", "99.99"}, {"100", "10", "90"}, {"100.05", "10.01", "90.04"}, {"150", "15", "135"}
        };
        for (String[] example : examples) {
            var order = new Order(UUID.randomUUID(), List.of(line(UUID.randomUUID(), example[0], 1)));
            assertThat(order.getDiscount()).isEqualTo(Money.of(example[1]));
            assertThat(order.getTotal()).isEqualTo(Money.of(example[2]));
        }
    }

    @Test
    void consolidatesAndProtectsOwnedItems() {
        UUID product = UUID.randomUUID();
        var order = new Order(UUID.randomUUID(), List.of(line(product, "25", 2), line(product, "25", 3)));
        assertThat(order.getItems()).hasSize(1);
        var item = order.getItems().getFirst();
        assertThat(item.getOrder()).isSameAs(order);
        assertThat(item.getId()).isNull();
        assertThat(item.getQuantity()).isEqualTo(5);
        assertThat(item.getLineTotal()).isEqualTo(Money.of("125"));
        var items = order.getItems();
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(items::clear);
    }

    @Test
    void rejectsEmptyInvalidAndExcessConsolidatedQuantities() {
        UUID product = UUID.randomUUID();
        assertThatIllegalArgumentException().isThrownBy(() -> new Order(UUID.randomUUID(), List.of()));
        for (int quantity : new int[] {0, -1, 10001, Integer.MAX_VALUE}) {
            assertThatIllegalArgumentException().isThrownBy(() -> line(product, "1", quantity));
        }
        new Order(UUID.randomUUID(), List.of(line(product, "1", 6000), line(product, "1", 4000)));
        assertThatIllegalArgumentException()
                .isThrownBy(() ->
                        new Order(UUID.randomUUID(), List.of(line(product, "1", 6000), line(product, "1", 4001))));
        assertThatNullPointerException().isThrownBy(() -> new Order(null, List.of(line(product, "1", 1))));
    }

    @Test
    void calculatesLargeMultiProductAmountsIndependently() {
        var order = new Order(
                UUID.randomUUID(),
                List.of(line(UUID.randomUUID(), "999999.99", 10000), line(UUID.randomUUID(), "999999.99", 10000)));
        assertThat(order.getItems().getFirst().getLineTotal()).isEqualTo(Money.of("9999999900.00"));
        assertThat(order.getSubtotal()).isEqualTo(Money.of("19999999800.00"));
        assertThat(order.getDiscount()).isEqualTo(Money.of("1999999980.00"));
        assertThat(order.getTotal()).isEqualTo(Money.of("17999999820.00"));
    }

    // Finding 1: AC-006 and the accepted consistent trusted-facts construction boundary.
    @ParameterizedTest(name = "{0}, reversed={3}")
    @MethodSource("conflictingDuplicates")
    void rejectsConflictingDuplicateFactsWithoutPublishingAPartialAggregate(
            String scenario, String secondName, String secondPrice, boolean reversed) {
        UUID product = UUID.randomUUID();
        var original = new OrderLineInput(product, "original", Money.of("25.00"), 2);
        var inconsistent = new OrderLineInput(product, secondName, Money.of(secondPrice), 3);
        List<OrderLineInput> inputs = reversed ? List.of(inconsistent, original) : List.of(original, inconsistent);
        var result = new AtomicReference<Order>();

        assertThatIllegalArgumentException().isThrownBy(() -> result.set(new Order(UUID.randomUUID(), inputs)));
        assertThat(result.get()).isNull(); // Construction returned no aggregate; no callback publishes this.
        assertThat(original.productName()).isEqualTo("original");
        assertThat(original.unitPrice()).isEqualTo(Money.of("25.00"));
        assertThat(original.quantity()).isEqualTo(2);
        assertThat(inputs)
                .containsExactlyElementsOf(
                        reversed ? List.of(inconsistent, original) : List.of(original, inconsistent));
    }

    static Stream<Arguments> conflictingDuplicates() {
        return Stream.of(false, true)
                .flatMap(reversed -> Stream.of(
                        Arguments.of("price conflict", "original", "26.00", reversed),
                        Arguments.of("name conflict", "renamed", "25.00", reversed),
                        Arguments.of("name and price conflict", "renamed", "26.00", reversed)));
    }

    @Test
    void consolidatesAtMaximumQuantityWithIndependentExpectedAmounts() {
        UUID product = UUID.randomUUID();
        var inputs = List.of(line(product, "1.00", 6000), line(product, "1.00", 4000));
        Order order = new Order(UUID.randomUUID(), inputs);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().getFirst().getQuantity()).isEqualTo(10000);
        assertThat(order.getItems().getFirst().getLineTotal()).isEqualTo(Money.of("10000.00"));
        assertThat(order.getSubtotal()).isEqualTo(Money.of("10000.00"));
        assertThat(order.getDiscount()).isEqualTo(Money.of("1000.00"));
        assertThat(order.getTotal()).isEqualTo(Money.of("9000.00"));
    }

    @Test
    void rejectsQuantityOverflowOnlyReachedAtThirdDuplicate() {
        UUID product = UUID.randomUUID();
        var firstTwo = List.of(line(product, "1.00", 4000), line(product, "1.00", 4000));
        assertThat(new Order(UUID.randomUUID(), firstTwo).getItems().getFirst().getQuantity())
                .isEqualTo(8000);
        var result = new AtomicReference<Order>();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> result.set(new Order(
                        UUID.randomUUID(),
                        List.of(firstTwo.getFirst(), firstTwo.getLast(), line(product, "1.00", 2001)))));
        assertThat(result.get()).isNull();
    }

    @Test
    void preservesDistinctProductsAndProducesSameFactsForReversedDuplicates() {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID(), customer = UUID.randomUUID();
        var inputs = List.of(line(first, "25.00", 2), line(second, "25.00", 4), line(first, "25.00", 3));
        var forward = new Order(customer, inputs);
        var backward = new Order(customer, inputs.reversed());
        Map<UUID, UnitSnapshot> expected = Map.of(
                first, new UnitSnapshot("original", Money.of("25.00"), 5, Money.of("125.00")),
                second, new UnitSnapshot("original", Money.of("25.00"), 4, Money.of("100.00")));
        for (Order order : List.of(forward, backward)) {
            assertThat(order.getCustomerId()).isEqualTo(customer);
            assertThat(order.getItems()).hasSize(2);
            assertThat(itemFacts(order)).isEqualTo(expected);
            assertThat(order.getSubtotal()).isEqualTo(Money.of("225.00"));
            assertThat(order.getDiscount()).isEqualTo(Money.of("22.50"));
            assertThat(order.getTotal()).isEqualTo(Money.of("202.50"));
        }
    }

    // Finding 2: BR-010/AC-009/AC-015. A snapshot does not hold a mutable Product graph.
    @Test
    void retainsEveryHistoricalFactAcrossProductRenameRepriceAndDeactivation() {
        var product = new Product("Original", Money.of("25.00"));
        UUID productId = UUID.randomUUID(); // Domain-only identity; provider-generated identity is tested in IT.
        var order = new Order(
                UUID.randomUUID(), List.of(new OrderLineInput(productId, product.getName(), product.getPrice(), 5)));
        product.update("Renamed", Money.of("40.00"));
        product.deactivate();
        product.update("Changed while inactive", Money.zero());

        var item = order.getItems().getFirst();
        assertThat(product.isActive()).isFalse();
        assertThat(product.getName()).isEqualTo("Changed while inactive");
        assertThat(product.getPrice()).isEqualTo(Money.zero());
        assertThat(item.getProductId()).isEqualTo(productId);
        assertThat(item.getProductName()).isEqualTo("Original");
        assertThat(item.getUnitPrice()).isEqualTo(Money.of("25.00"));
        assertThat(item.getQuantity()).isEqualTo(5);
        assertThat(item.getLineTotal()).isEqualTo(Money.of("125.00"));
        assertThat(order.getSubtotal()).isEqualTo(Money.of("125.00"));
        assertThat(order.getDiscount()).isEqualTo(Money.of("12.50"));
        assertThat(order.getTotal()).isEqualTo(Money.of("112.50"));
    }

    @ParameterizedTest(name = "item collection rejects {0}")
    @MethodSource("structuralMutations")
    void prohibitsStructuralMutationThroughItemGetter(String operation, Consumer<List<OrderItem>> mutation) {
        var order = new Order(UUID.randomUUID(), List.of(line(UUID.randomUUID(), "25.00", 2)));
        var item = order.getItems().getFirst();
        var items = order.getItems();
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> mutation.accept(items));
        assertThat(order.getItems()).containsExactly(item);
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getLineTotal()).isEqualTo(Money.of("50.00"));
        assertThat(order.getSubtotal()).isEqualTo(Money.of("50.00"));
        assertThat(order.getDiscount()).isEqualTo(Money.zero());
        assertThat(order.getTotal()).isEqualTo(Money.of("50.00"));
    }

    static Stream<Arguments> structuralMutations() {
        return Stream.of(
                Arguments.of("add", (Consumer<List<OrderItem>>) items -> items.add(items.getFirst())),
                Arguments.of("remove", (Consumer<List<OrderItem>>) items -> items.remove(0)),
                Arguments.of("replace", (Consumer<List<OrderItem>>) items -> items.set(0, items.getFirst())),
                Arguments.of("clear", (Consumer<List<OrderItem>>) List::clear),
                Arguments.of("iterator removal", (Consumer<List<OrderItem>>) items -> {
                    var iterator = items.iterator();
                    iterator.next();
                    iterator.remove();
                }),
                Arguments.of("sublist removal", (Consumer<List<OrderItem>>)
                        items -> items.subList(0, 1).clear()));
    }

    @Test
    void exposesNoIndependentPublicOrderItemSnapshotMutator() {
        // Inspect declared public business methods, excluding inherited Object/provider enhancement methods.
        var publicBusinessMethods = Arrays.stream(OrderItem.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers())
                        && !method.isSynthetic()
                        && !method.getName().startsWith("$$_hibernate_"))
                .toList();
        assertThat(publicBusinessMethods).isNotEmpty().allSatisfy(method -> {
            assertThat(method.getParameterCount()).as(method.getName()).isZero();
            assertThat(method.getReturnType()).as(method.getName()).isNotEqualTo(void.class);
        });
        assertThat(Arrays.stream(OrderItem.class.getDeclaredFields())
                        .filter(field -> !field.isSynthetic() && !Modifier.isStatic(field.getModifiers())))
                .allSatisfy(field ->
                        assertThat(Modifier.isPublic(field.getModifiers())).isFalse());
        assertThat(OrderItem.class.getConstructors()).isEmpty();
    }

    // Finding 3: BR-001/BR-005/BR-011/BR-013. No catalog existence/current-state claims.
    @Test
    void rejectsNullTrustedLineFactsAndNullAggregateInputs() {
        UUID id = UUID.randomUUID();
        assertThatNullPointerException().isThrownBy(() -> new OrderLineInput(null, "name", Money.zero(), 1));
        assertThatNullPointerException().isThrownBy(() -> new OrderLineInput(id, null, Money.zero(), 1));
        assertThatNullPointerException().isThrownBy(() -> new OrderLineInput(id, "name", null, 1));
        assertThatNullPointerException().isThrownBy(() -> new Order(UUID.randomUUID(), null));
        assertThatNullPointerException()
                .isThrownBy(() -> new Order(UUID.randomUUID(), Arrays.asList((OrderLineInput) null)));
    }

    @ParameterizedTest
    @MethodSource("invalidTrustedNames")
    void rejectsInvalidTrustedNames(String name) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OrderLineInput(UUID.randomUUID(), name, Money.zero(), 1));
    }

    static Stream<String> invalidTrustedNames() {
        return Stream.of("", " ", "\u2003\t", "😀".repeat(201), "\uD800", "\uDC00", "\uDFFF", "a\uD800b");
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "😀"})
    void acceptsExactlyOneValidUnicodeCodePointName(String name) {
        var input = new OrderLineInput(UUID.randomUUID(), name, Money.zero(), 1);
        assertThat(input.productName()).isEqualTo(name);
    }

    @Test
    void stripsNamesAndPreservesExactlyTwoHundredSupplementaryCodePoints() {
        UUID id = UUID.randomUUID();
        String name = "😀".repeat(199) + "Ä";
        var price = Money.of("999999.99");
        var input = new OrderLineInput(id, "\u2003\t" + name + " \n", price, 10000);
        assertThat(input.productId()).isEqualTo(id);
        assertThat(input.productName()).isEqualTo(name);
        assertThat(input.productName().codePointCount(0, input.productName().length()))
                .isEqualTo(200);
        assertThat(input.unitPrice()).isSameAs(price);
        assertThat(input.quantity()).isEqualTo(10000);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "1000000.00"})
    void rejectsTrustedPricesOutsideProductRange(String price) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OrderLineInput(UUID.randomUUID(), "name", Money.of(price), 1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "999999.99"})
    void acceptsTrustedPriceEndpoints(String price) {
        var input = new OrderLineInput(UUID.randomUUID(), "name", Money.of(price), 1);
        assertThat(input.unitPrice()).isEqualTo(Money.of(price));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.001", "10.000"})
    void rejectsExcessSourcePriceScaleBeforeTrustedCarrierConstruction(String price) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OrderLineInput(UUID.randomUUID(), "name", Money.of(price), 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE, 10001, Integer.MAX_VALUE})
    void rejectsInvalidTrustedQuantities(int quantity) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OrderLineInput(UUID.randomUUID(), "name", Money.of("1"), quantity));
    }

    @Test
    void handlesZeroValueOrderAndDiscountBasedOnMultipleDistinctLines() {
        var free = new Order(UUID.randomUUID(), List.of(line(UUID.randomUUID(), "0", 10000)));
        assertThat(free.getSubtotal()).isEqualTo(Money.zero());
        assertThat(free.getDiscount()).isEqualTo(Money.zero());
        assertThat(free.getTotal()).isEqualTo(Money.zero());
        var order = new Order(
                UUID.randomUUID(), List.of(line(UUID.randomUUID(), "50.02", 1), line(UUID.randomUUID(), "50.03", 1)));
        assertThat(order.getSubtotal()).isEqualTo(Money.of("100.05"));
        assertThat(order.getDiscount()).isEqualTo(Money.of("10.01"));
        assertThat(order.getTotal()).isEqualTo(Money.of("90.04"));
    }

    private Map<UUID, UnitSnapshot> itemFacts(Order order) {
        return order.getItems().stream()
                .collect(Collectors.toMap(
                        OrderItem::getProductId,
                        item -> new UnitSnapshot(
                                item.getProductName(), item.getUnitPrice(), item.getQuantity(), item.getLineTotal())));
    }

    private record UnitSnapshot(String name, Money unitPrice, int quantity, Money lineTotal) {}
}
