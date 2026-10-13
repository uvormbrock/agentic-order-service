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
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "order_items", uniqueConstraints = @UniqueConstraint(name = "uq_order_items_product", columnNames = {"order_id", "product_id"}))
@Access(AccessType.FIELD)
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(name = "product_name", nullable = false, columnDefinition = "text")
    private String productName;
    @Column(nullable = false)
    private int quantity;
    @Column(name = "unit_price", nullable = false, columnDefinition = "numeric")
    private Money unitPrice;
    @Column(name = "line_total", nullable = false, columnDefinition = "numeric")
    private Money lineTotal;

    protected OrderItem() { }
    OrderItem(Order order, OrderLineInput input) {
        this.order = order;
        productId = input.productId();
        productName = input.productName();
        quantity = input.quantity();
        unitPrice = input.unitPrice();
        lineTotal = unitPrice.multiply(quantity);
    }

    public UUID getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public Money getLineTotal() {
        return lineTotal;
    }
}
