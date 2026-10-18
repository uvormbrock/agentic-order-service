package com.example.agenticorderservice.product.storage;

import com.example.agenticorderservice.product.application.ProductImageFailure;
import com.example.agenticorderservice.product.application.ProductImageProcessor;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProductImageStorage {
    private final Path root;

    public record StagedImage(String originalKey, String thumbnailKey) {}

    public ProductImageStorage(
            @Value("${app.product-images.storage-directory:./var/product-images}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }

    public StagedImage stage(ProductImageProcessor.ProcessedImage image) {
        String generation = UUID.randomUUID().toString();
        boolean created = false;
        try {
            ensureRoot();
            Path directory = root.resolve(generation);
            Files.createDirectory(directory);
            created = true;
            Files.write(
                    directory.resolve("original"),
                    image.original(),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
            Files.write(
                    directory.resolve("thumbnail"),
                    image.thumbnail(),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
            return new StagedImage(generation + "/original", generation + "/thumbnail");
        } catch (IOException | SecurityException failure) {
            if (created) {
                try {
                    deleteGeneration(generation + "/original");
                } catch (ProductImageFailure cleanup) {
                    failure.addSuppressed(cleanup);
                }
            }
            throw storageFailure(failure);
        }
    }

    public byte[] read(String key, long expectedSize) {
        try {
            Path file = checkedFile(key);
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                    || Files.size(file) != expectedSize
                    || expectedSize <= 0
                    || expectedSize > ProductImageProcessor.MAX_BYTES)
                throw new IOException("Invalid managed image file");
            try (var input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
                byte[] bytes = input.readNBytes((int) expectedSize + 1);
                if (bytes.length != expectedSize) throw new IOException("Managed image size changed");
                return bytes;
            }
        } catch (IOException | SecurityException failure) {
            throw storageFailure(failure);
        }
    }

    public void deleteGeneration(String originalKey) {
        try {
            Path original = checkedFile(originalKey);
            Path directory = original.getParent();
            if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) return;
            Files.deleteIfExists(original);
            Path thumbnail = checkedFile(originalKey.substring(0, 36) + "/thumbnail");
            Files.deleteIfExists(thumbnail);
            Files.delete(directory);
        } catch (IOException | SecurityException failure) {
            throw storageFailure(failure);
        }
    }

    /** Caller must stop all readers/writers across every instance before invoking reconciliation. */
    public int reconcile(Set<String> referencedOriginalKeys) {
        try {
            ensureRoot();
            int removed = 0;
            try (var entries = Files.newDirectoryStream(root)) {
                for (Path entry : entries) {
                    String generation = entry.getFileName().toString();
                    if (generation.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
                            && !referencedOriginalKeys.contains(generation + "/original")) {
                        deleteGeneration(generation + "/original");
                        removed++;
                    }
                }
            }
            return removed;
        } catch (IOException | SecurityException failure) {
            throw storageFailure(failure);
        }
    }

    private Path checkedFile(String key) throws IOException {
        if (key == null
                || !key.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/(original|thumbnail)"))
            throw new IOException("Invalid managed key");
        checkAncestors(root);
        Path file = root.resolve(key).normalize();
        if (!file.startsWith(root) || Files.isSymbolicLink(file.getParent()) || Files.isSymbolicLink(file))
            throw new IOException("Unsafe managed path");
        return file;
    }

    private void ensureRoot() throws IOException {
        checkAncestors(root);
        Files.createDirectories(root);
        checkAncestors(root);
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Invalid image root");
    }

    private void checkAncestors(Path path) throws IOException {
        for (Path current = path; current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current)) throw new IOException("Symbolic image root");
        }
    }

    private ProductImageFailure storageFailure(Exception cause) {
        return new ProductImageFailure(ProductImageFailure.Reason.STORAGE, cause);
    }
}
