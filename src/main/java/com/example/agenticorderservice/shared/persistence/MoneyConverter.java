package com.example.agenticorderservice.shared.persistence;

import com.example.agenticorderservice.shared.domain.Money;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;

@Converter(autoApply = true)
public class MoneyConverter implements AttributeConverter<Money, BigDecimal> {
    @Override
    public BigDecimal convertToDatabaseColumn(Money attribute) {
        return attribute == null ? null : attribute.amount();
    }
    @Override
    public Money convertToEntityAttribute(BigDecimal value) {
        return value == null ? null : new Money(value);
    }
}
