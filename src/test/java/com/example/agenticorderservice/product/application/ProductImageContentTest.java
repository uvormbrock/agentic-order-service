package com.example.agenticorderservice.product.application;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ProductImageContentTest {
    @Test
    void equalityAndHashCodeUseBinaryContentAndMediaWhileSummaryContainsOnlyLength() {
        byte[] bytes = "private image payload".getBytes(StandardCharsets.UTF_8);
        var content = new ProductImageContent(bytes, "image/png");
        var same = new ProductImageContent(bytes.clone(), "image/png");
        assertThat(content).isEqualTo(same).hasSameHashCodeAs(same);
        assertThat(same).isEqualTo(content);
        assertThat(content).isNotEqualTo(new ProductImageContent(new byte[] {1}, "image/png"));
        assertThat(content).isNotEqualTo(new ProductImageContent(bytes, "image/jpeg"));
        assertThat(content).hasToString("ProductImageContent[byteLength=21, mediaType=image/png]");
    }

    @Test
    void inputAndReturnedArraysCannotMutateTheStoredBinaryValue() {
        byte[] bytes = {1, 2, 3};
        var content = new ProductImageContent(bytes, "image/png");
        var expected = new ProductImageContent(new byte[] {1, 2, 3}, "image/png");
        bytes[0] = 9;
        byte[] returned = content.bytes();
        returned[1] = 9;
        assertThat(content.bytes()).containsExactly(1, 2, 3);
        assertThat(content).isEqualTo(expected).hasSameHashCodeAs(expected);
    }
}
