package com.example.agenticorderservice.customer.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Version;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "customers")
@Access(AccessType.FIELD)
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, columnDefinition = "text")
    private String name;
    @Column(nullable = false, columnDefinition = "text")
    private EmailAddress email;
    @Column(nullable = false)
    private boolean active = true;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Customer() { }
    public Customer(String name, EmailAddress email) {
        update(name, email);
    }

    public void update(String name, EmailAddress email) {
        String normalized = Objects.requireNonNull(name, "name").strip();
        int length = normalized.codePointCount(0, normalized.length());
        if (normalized.codePoints().anyMatch(c -> c >= 0xD800 && c <= 0xDFFF)
                || length < 1 || length > 200) {
            throw new IllegalArgumentException("Name requires 1–200 code points");
        }
        Objects.requireNonNull(email, "email");
        this.name = normalized;
        this.email = email;
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

    public EmailAddress getEmail() {
        return email;
    }

    public boolean isActive() {
        return active;
    }
}
