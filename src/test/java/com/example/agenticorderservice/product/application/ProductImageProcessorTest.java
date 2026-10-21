package com.example.agenticorderservice.product.application;

import static com.example.agenticorderservice.product.image.ProductImageFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.product.storage.ProductImageProcessor;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProductImageProcessorTest {
    private final ProductImageProcessor processor = new ProductImageProcessor();

    @ParameterizedTest
    @CsvSource({"png,960,480,320,160", "jpg,480,960,160,320", "png,25,40,25,40", "png,1,1000,1,320"})
    void preservesOriginalBytesAndGeneratesDecodableAspectRatioThumbnail(
            String format, int width, int height, int tw, int th) throws Exception {
        byte[] bytes = image(format, width, height);
        var result = processor.process(bytes, format.equals("jpg") ? "image/jpeg" : "image/png");
        assertThat(result.original()).containsExactly(bytes).isNotSameAs(bytes);
        assertThat(result.width()).isEqualTo(width);
        assertThat(result.height()).isEqualTo(height);
        assertThat(result.thumbnailWidth()).isEqualTo(tw);
        assertThat(result.thumbnailHeight()).isEqualTo(th);
        var original = ImageIO.read(new ByteArrayInputStream(result.original()));
        var thumbnail = ImageIO.read(new ByteArrayInputStream(result.thumbnail()));
        assertThat(original.getWidth()).isEqualTo(width);
        assertThat(original.getHeight()).isEqualTo(height);
        assertThat(thumbnail.getWidth()).isEqualTo(tw);
        assertThat(thumbnail.getHeight()).isEqualTo(th);
        if (width > 1) {
            assertThat(thumbnail.getRGB(0, 0)).isNotEqualTo(thumbnail.getRGB(tw - 1, th - 1));
        }
        bytes[0] = 0;
        assertThat(result.original()[0]).isNotZero();
    }

    @Test
    void processedRecordUsesArrayContentsAndEveryMetadataComponentWithoutPrintingBytes() {
        byte[] original = {11, 22, 33};
        byte[] thumbnail = {44, 55};
        var value = new ProcessedImage(original, thumbnail, "image/png", 40, 20, 20, 10);
        var same = new ProcessedImage(original.clone(), thumbnail.clone(), "image/png", 40, 20, 20, 10);
        assertThat(value).isEqualTo(same).hasSameHashCodeAs(same);
        assertThat(same).isEqualTo(value);
        var alternatives = java.util.List.of(
                new ProcessedImage(new byte[] {11}, thumbnail, "image/png", 40, 20, 20, 10),
                new ProcessedImage(original, new byte[] {44}, "image/png", 40, 20, 20, 10),
                new ProcessedImage(original, thumbnail, "image/jpeg", 40, 20, 20, 10),
                new ProcessedImage(original, thumbnail, "image/png", 41, 20, 20, 10),
                new ProcessedImage(original, thumbnail, "image/png", 40, 21, 20, 10),
                new ProcessedImage(original, thumbnail, "image/png", 40, 20, 21, 10),
                new ProcessedImage(original, thumbnail, "image/png", 40, 20, 20, 11));
        for (var alternative : alternatives) assertThat(value).isNotEqualTo(alternative);
        assertThat(value.toString())
                .contains("originalLength=3", "thumbnailLength=2")
                .doesNotContain(Arrays.toString(original), Arrays.toString(thumbnail));
    }

    @Test
    void readerIsDisposedAfterSuccessfulProcessingAndRejectedFormat() {
        byte[] bytes = image("png", 4, 4);
        for (String declaredType : java.util.List.of("image/png", "image/jpeg")) {
            var reader = spy(ImageIO.getImageReadersByFormatName("png").next());
            try (var imageIo = mockStatic(ImageIO.class, CALLS_REAL_METHODS)) {
                imageIo.when(() -> ImageIO.getImageReaders(any(Object.class)))
                        .thenReturn(java.util.List.of(reader).iterator());
                if (declaredType.equals("image/png")) {
                    processor.process(bytes, declaredType);
                } else {
                    rejected(bytes, declaredType, ProductImageFailure.Reason.UNSUPPORTED);
                }
                verify(reader).dispose();
            }
        }
    }

    @Test
    void acceptsExactByteLimitWithoutRecompressingOriginal() {
        byte[] bytes = Arrays.copyOf(image("png", 2, 2), 10 * 1024 * 1024);
        assertThat(processor.process(bytes, "image/png").original()).containsExactly(bytes);
        rejected(Arrays.copyOf(bytes, bytes.length + 1), "image/png", ProductImageFailure.Reason.TOO_LARGE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/gif", "application/octet-stream", "", "text/plain"})
    void rejectsUnsupportedDeclaredMedia(String media) {
        rejected(image("png", 4, 4), media, ProductImageFailure.Reason.UNSUPPORTED);
    }

    @Test
    void rejectsSpoofedMediaAndUnsupportedEncodedFormat() {
        rejected(image("png", 4, 4), "image/jpeg", ProductImageFailure.Reason.UNSUPPORTED);
        rejected(image("gif", 4, 4), "image/png", ProductImageFailure.Reason.UNSUPPORTED);
    }

    @Test
    void rejectsEmptyCorruptAndTruncatedImages() {
        rejected(new byte[0], "image/png", ProductImageFailure.Reason.INVALID);
        rejected(new byte[] {1, 2, 3}, "image/png", ProductImageFailure.Reason.INVALID);
        rejected(Arrays.copyOf(image("png", 4, 4), 40), "image/png", ProductImageFailure.Reason.INVALID);
    }

    @ParameterizedTest
    @CsvSource({"8001,5000", "16385,1", "1,16385", "2147483647,2147483647"})
    void rejectsDimensionAndPixelLimitsBeforeDecoding(int width, int height) {
        rejected(pngWithDimensions(width, height), "image/png", ProductImageFailure.Reason.INVALID);
    }

    @ParameterizedTest
    @CsvSource({"8001,5000", "16385,1", "1,16385"})
    void rejectsExcessiveAllocationBeforeInvokingDecoder(int width, int height) throws Exception {
        byte[] bytes = pngWithDimensions(width, height);
        var reader = spy(ImageIO.getImageReadersByFormatName("png").next());
        try (var imageIo = mockStatic(ImageIO.class, CALLS_REAL_METHODS)) {
            imageIo.when(() -> ImageIO.getImageReaders(any(Object.class)))
                    .thenReturn(java.util.List.of(reader).iterator());
            rejected(bytes, "image/png", ProductImageFailure.Reason.INVALID);
            verify(reader, never()).read(0);
            verify(reader).dispose();
        }
    }

    @ParameterizedTest
    @CsvSource({"16384,1", "1,16384"})
    void acceptsMaximumSingleEdgeAndNeverUpscalesTheShortEdge(int width, int height) {
        var result = processor.process(image("png", width, height), "image/png");
        assertThat(result.width()).isEqualTo(width);
        assertThat(result.height()).isEqualTo(height);
        assertThat(Math.max(result.thumbnailWidth(), result.thumbnailHeight())).isEqualTo(320);
        assertThat(Math.min(result.thumbnailWidth(), result.thumbnailHeight())).isEqualTo(1);
    }

    private void rejected(byte[] bytes, String media, ProductImageFailure.Reason reason) {
        assertThatThrownBy(() -> processor.process(bytes, media))
                .isInstanceOfSatisfying(
                        ProductImageFailure.class,
                        failure -> assertThat(failure.reason()).isEqualTo(reason));
    }
}
