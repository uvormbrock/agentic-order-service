package com.example.agenticorderservice.product.image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;

public final class ProductImageFixtures {
    private ProductImageFixtures() {}

    public static byte[] image(String format, int width, int height) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(Color.BLUE);
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(Color.ORANGE);
            graphics.fillRect(0, 0, width / 2, height / 2);
        } finally {
            graphics.dispose();
        }
        try (var output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, format, output)) {
                throw new AssertionError("Missing fixture encoder: " + format);
            }
            return output.toByteArray();
        } catch (IOException failure) {
            throw new AssertionError(failure);
        }
    }

    public static byte[] pngWithDimensions(int width, int height) {
        byte[] png = image("png", 1, 1);
        ByteBuffer.wrap(png).putInt(16, width).putInt(20, height);
        var crc = new CRC32();
        crc.update(png, 12, 17);
        ByteBuffer.wrap(png).putInt(29, (int) crc.getValue());
        return png;
    }
}
