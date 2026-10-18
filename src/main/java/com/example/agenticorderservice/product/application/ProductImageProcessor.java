package com.example.agenticorderservice.product.application;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.stereotype.Component;

@Component
public class ProductImageProcessor {
    public static final int MAX_BYTES = 10 * 1024 * 1024;
    public static final long MAX_PIXELS = 40_000_000L;
    public static final int MAX_EDGE = 16_384;
    public static final int THUMBNAIL_EDGE = 320;

    public record ProcessedImage(
            byte[] original,
            byte[] thumbnail,
            String mediaType,
            int width,
            int height,
            int thumbnailWidth,
            int thumbnailHeight) {}

    public ProcessedImage process(byte[] bytes, String declaredType) {
        if (bytes.length > MAX_BYTES) throw new ProductImageFailure(ProductImageFailure.Reason.TOO_LARGE);
        if (bytes.length == 0) throw new ProductImageFailure(ProductImageFailure.Reason.INVALID);
        if (!"image/jpeg".equalsIgnoreCase(declaredType) && !"image/png".equalsIgnoreCase(declaredType)) {
            throw new ProductImageFailure(ProductImageFailure.Reason.UNSUPPORTED);
        }
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ProductImageFailure(ProductImageFailure.Reason.INVALID);
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                String media = "JPEG".equalsIgnoreCase(format)
                        ? "image/jpeg"
                        : "PNG".equalsIgnoreCase(format) ? "image/png" : "";
                if (media.isEmpty() || !media.equalsIgnoreCase(declaredType))
                    throw new ProductImageFailure(ProductImageFailure.Reason.UNSUPPORTED);
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1
                        || height < 1
                        || width > MAX_EDGE
                        || height > MAX_EDGE
                        || (long) width * height > MAX_PIXELS)
                    throw new ProductImageFailure(ProductImageFailure.Reason.INVALID);
                BufferedImage original = reader.read(0);
                if (original == null) throw new ProductImageFailure(ProductImageFailure.Reason.INVALID);
                double scale = Math.min(1.0, (double) THUMBNAIL_EDGE / Math.max(width, height));
                int tw = Math.max(1, (int) Math.round(width * scale));
                int th = Math.max(1, (int) Math.round(height * scale));
                BufferedImage thumb = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
                var graphics = thumb.createGraphics();
                try {
                    graphics.setRenderingHint(
                            RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    graphics.drawImage(original, 0, 0, tw, th, null);
                } finally {
                    graphics.dispose();
                }
                var output = new ByteArrayOutputStream();
                if (!ImageIO.write(thumb, "png", output))
                    throw new ProductImageFailure(ProductImageFailure.Reason.INVALID);
                return new ProcessedImage(bytes.clone(), output.toByteArray(), media, width, height, tw, th);
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException failure) {
            throw new ProductImageFailure(ProductImageFailure.Reason.INVALID, failure);
        }
    }
}
