package com.example.agenticorderservice.product.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.product.application.port.out.FindProductById;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GetProductServiceTest {
    private final FindProductById output = mock(FindProductById.class);
    private final GetProductService service = new GetProductService(output);

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void returnsCurrentFactsIncludingInactiveProducts(boolean active) {
        UUID id = UUID.randomUUID();
        var expected = new ProductResult(id, "Äpfel😀", new BigDecimal("12.34"), active);
        when(output.findById(id)).thenReturn(Optional.of(expected));
        assertThat(service.get(id)).isEqualTo(expected);
        verify(output).findById(id);
        verifyNoMoreInteractions(output);
    }

    @Test
    void missingProductRaisesExistingFeatureException() {
        UUID id = UUID.randomUUID();
        when(output.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(id)).isInstanceOf(ProductNotFoundException.class);
        verify(output).findById(id);
        verifyNoMoreInteractions(output);
    }
}
