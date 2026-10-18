package com.example.agenticorderservice.product.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProductImageTest {
    @ParameterizedTest
    @CsvSource({"1,1,1,1,1", "10485760,16384,1,320,1", "10485760,1,16384,1,320", "10,8000,5000,320,200"})
    void acceptsExactApprovedMetadataBoundaries(long size, int width, int height, int tw, int th) {
        String key = UUID.randomUUID().toString();
        var image = new ProductImage(
                UUID.randomUUID(), key + "/original", key + "/thumbnail", "image/png", size, 1, width, height, tw, th);
        assertThat(image.getOriginalSize()).isEqualTo(size);
        assertThat(image.getWidth()).isEqualTo(width);
        assertThat(image.getHeight()).isEqualTo(height);
        assertThat(image.getThumbnailWidth()).isEqualTo(tw);
        assertThat(image.getThumbnailHeight()).isEqualTo(th);
    }

    @ParameterizedTest
    @CsvSource({
        "0,20,30,20,30",
        "10485761,20,30,20,30",
        "1,0,30,1,30",
        "1,20,0,20,1",
        "1,16385,1,320,1",
        "1,1,16385,1,320",
        "1,8001,5000,320,200",
        "1,320,320,0,320",
        "1,320,320,320,0",
        "1,321,321,321,320",
        "1,321,321,320,321",
        "1,20,30,20,31"
    })
    void rejectsUnsupportedMetadataBoundaries(long size, int width, int height, int tw, int th) {
        String key = UUID.randomUUID().toString();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ProductImage(
                        UUID.randomUUID(),
                        key + "/original",
                        key + "/thumbnail",
                        "image/png",
                        size,
                        1,
                        width,
                        height,
                        tw,
                        th));
    }

    @Test
    void rejectsMismatchedGenerationsUnsupportedMediaAndEmptyThumbnail() {
        String key = UUID.randomUUID().toString();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ProductImage(
                        UUID.randomUUID(),
                        key + "/original",
                        UUID.randomUUID() + "/thumbnail",
                        "image/png",
                        1,
                        1,
                        1,
                        1,
                        1,
                        1));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ProductImage(
                        UUID.randomUUID(), key + "/original", key + "/thumbnail", "image/gif", 1, 1, 1, 1, 1, 1));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ProductImage(
                        UUID.randomUUID(), key + "/original", key + "/thumbnail", "image/png", 1, 0, 1, 1, 1, 1));
    }

    @Test
    void replacementPreservesProductIdentityAndRejectsInvalidMetadataBeforeMutation() {
        UUID product = UUID.randomUUID();
        String first = UUID.randomUUID().toString();
        String second = UUID.randomUUID().toString();
        var image = new ProductImage(
                product, first + "/original", first + "/thumbnail", "image/jpeg", 100, 50, 640, 320, 320, 160);
        image.replace(second + "/original", second + "/thumbnail", "image/png", 200, 60, 20, 30, 20, 30);
        assertThat(image.getProductId()).isEqualTo(product);
        assertThat(image.getOriginalKey()).isEqualTo(second + "/original");
        assertThat(image.getThumbnailKey()).isEqualTo(second + "/thumbnail");
        assertThat(image.getMediaType()).isEqualTo("image/png");
        assertThat(image.getOriginalSize()).isEqualTo(200);
        assertThat(image.getThumbnailSize()).isEqualTo(60);
        assertThat(image.getWidth()).isEqualTo(20);
        assertThat(image.getHeight()).isEqualTo(30);
        assertThat(image.getThumbnailWidth()).isEqualTo(20);
        assertThat(image.getThumbnailHeight()).isEqualTo(30);
        assertThatIllegalArgumentException()
                .isThrownBy(() ->
                        image.replace(first + "/original", first + "/thumbnail", "image/png", 200, 60, 20, 30, 21, 30));
        assertThat(image.getOriginalKey()).isEqualTo(second + "/original");
        assertThatIllegalArgumentException()
                .isThrownBy(() ->
                        image.replace("../../original", first + "/thumbnail", "image/png", 200, 60, 20, 30, 20, 30));
        assertThat(image.getOriginalKey()).isEqualTo(second + "/original");
    }
}
