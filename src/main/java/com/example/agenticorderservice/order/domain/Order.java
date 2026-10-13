package com.example.agenticorderservice.order.domain;

import com.example.agenticorderservice.shared.domain.Money;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity(name = "PurchaseOrder")
@Table(name = "orders")
@Access(AccessType.FIELD)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY, orphanRemoval = false)
    private List<OrderItem> items = new ArrayList<>();
    @Column(nullable = false, columnDefinition = "numeric")
    private Money subtotal;
    @Column(nullable = false, columnDefinition = "numeric")
    private Money discount;
    @Column(nullable = false, columnDefinition = "numeric")
    private Money total;

    protected Order() { }
    public Order(UUID customerId, List<OrderLineInput> inputs) {
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        if (Objects.requireNonNull(inputs, "items").isEmpty()) {
            throw new IllegalArgumentException("Order requires items");
        }
        Map<UUID, OrderLineInput> consolidated = new LinkedHashMap<>();
        for (OrderLineInput input : inputs) {
            Objects.requireNonNull(input, "item");
            OrderLineInput prior = consolidated.get(input.productId());
            if (prior != null) {
                if (!prior.productName().equals(input.productName()) || !prior.unitPrice().equals(input.unitPrice())) {
                    throw new IllegalArgumentException("Conflicting trusted facts for one product");
                }
                input = new OrderLineInput(input.productId(), input.productName(), input.unitPrice(),
                        Math.addExact(prior.quantity(), input.quantity()));
            }
            consolidated.put(input.productId(), input);
        }
        subtotal = Money.zero();
        for (OrderLineInput input : consolidated.values()) {
            OrderItem item = new OrderItem(this, input);
            items.add(item);
            subtotal = subtotal.plus(item.getLineTotal());
        }
        discount = subtotal.compareTo(Money.of("100")) >= 0
                ? new Money(subtotal.amount().multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP))
                : Money.zero();
        total = subtotal.minus(discount);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Money getSubtotal() {
        return subtotal;
    }

    public Money getDiscount() {
        return discount;
    }

    public Money getTotal() {
        return total;
    }
}
