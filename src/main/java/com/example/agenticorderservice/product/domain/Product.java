package com.example.agenticorderservice.product.domain;

import com.example.agenticorderservice.shared.domain.Money;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "products")
@Access(AccessType.FIELD)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @Column(nullable = false, columnDefinition = "numeric")
    private Money price;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Product() {}

    public Product(String name, Money price) {
        update(name, price);
    }

    public void update(String name, Money price) {
        String normalized = Objects.requireNonNull(name, "name").strip();
        int length = normalized.codePointCount(0, normalized.length());
        if (normalized.codePoints().anyMatch(c -> c >= 0xD800 && c <= 0xDFFF) || length < 1 || length > 200) {
            throw new IllegalArgumentException("Name requires 1–200 code points");
        }
        Objects.requireNonNull(price, "price");
        if (price.compareTo(Money.zero()) < 0 || price.compareTo(Money.of("999999.99")) > 0) {
            throw new IllegalArgumentException("Product price outside approved range");
        }
        this.name = normalized;
        this.price = price;
    }

    public void deactivate() {
        active = false;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Money getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }
}
