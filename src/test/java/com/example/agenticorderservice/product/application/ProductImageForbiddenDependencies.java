package com.example.agenticorderservice.product.application;

import com.example.agenticorderservice.product.persistence.ProductImageRepository;
import com.example.agenticorderservice.product.storage.ProductImageStorage;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.springframework.transaction.PlatformTransactionManager;

/** Negative fixtures imported explicitly; never scanned as production components. */
public final class ProductImageForbiddenDependencies {
    private ProductImageForbiddenDependencies() {}

    public static class RepositoryAccess {
        ProductImageRepository images;
    }

    public static class FilesystemAccess {
        Path path;
    }

    public static class ImageIoAccess {
        ImageIO imageIo;
    }

    public static class StorageAccess {
        ProductImageStorage storage;
    }

    public static class AwtAccess {
        java.awt.image.BufferedImage image;
    }

    public static class TransactionAccess {
        PlatformTransactionManager manager;
    }
}
