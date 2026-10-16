package com.example.agenticorderservice.product.api;

import java.math.BigDecimal;

public record ProductDetailsRequest(String name, BigDecimal price) {}
