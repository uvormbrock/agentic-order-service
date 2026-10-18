package com.example.agenticorderservice.product.storage;

import static com.example.agenticorderservice.product.image.ProductImageFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.product.application.ProductImageFailure;
import com.example.agenticorderservice.product.application.ProductImageProcessor;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProductImageStorageTest {
    @TempDir
    Path temporary;

    ProductImageStorage storage() throws Exception {
        return new ProductImageStorage(temporary.toRealPath().resolve("managed").toString());
    }

    ProductImageProcessor.ProcessedImage processed() {
        return new ProductImageProcessor().process(image("png", 640, 320), "image/png");
    }

    @Test
    void generationsAreImmutableDistinctAndBothVariantsAreDeletedIdempotently() throws Exception {
        var storage = storage();
        var image = processed();
        var first = storage.stage(image);
        var second = storage.stage(image);
        assertThat(first.originalKey()).isNotEqualTo(second.originalKey()).doesNotStartWith("/");
        assertThat(storage.read(first.originalKey(), image.original().length)).containsExactly(image.original());
        assertThat(storage.read(first.thumbnailKey(), image.thumbnail().length)).containsExactly(image.thumbnail());
        storage.deleteGeneration(first.originalKey());
        storage.deleteGeneration(first.originalKey());
        assertThat(temporary.resolve("managed").resolve(first.originalKey()).getParent())
                .doesNotExist();
        assertThat(storage.read(second.originalKey(), image.original().length)).containsExactly(image.original());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"../secret", "/etc/passwd", "bad/original", "00000000-0000-0000-0000-000000000000/../../secret"})
    void rejectsTraversalAndNonManagedKeys(String key) throws Exception {
        var storage = storage();
        failure(() -> storage.read(key, 1));
        failure(() -> storage.deleteGeneration(key));
    }

    @Test
    void rejectsMissingFilesAndIncorrectSizes() throws Exception {
        var storage = storage();
        var image = processed();
        var keys = storage.stage(image);
        failure(() -> storage.read(keys.originalKey(), image.original().length + 1));
        failure(() -> storage.read(keys.originalKey(), 0));
        failure(() -> storage.read(UUID.randomUUID() + "/original", 1));
    }

    @Test
    void refusesRootAncestorGenerationAndFileSymlinksWithoutTouchingTargets() throws Exception {
        Path outside = Files.createDirectory(temporary.resolve("outside"));
        Path secret = Files.write(outside.resolve("secret"), new byte[] {7, 8});
        Path rootLink = temporary.resolve("root-link");
        Files.createSymbolicLink(rootLink, outside);
        failure(() -> new ProductImageStorage(rootLink.toString()).stage(processed()));
        failure(() -> new ProductImageStorage(rootLink.resolve("nested").toString()).stage(processed()));
        var storage = storage();
        var keys = storage.stage(processed());
        Path original = temporary.resolve("managed").resolve(keys.originalKey());
        Files.delete(original);
        Files.createSymbolicLink(original, secret);
        failure(() -> storage.read(keys.originalKey(), 2));
        failure(() -> storage.deleteGeneration(keys.originalKey()));
        Files.delete(original);
        Path generation = original.getParent();
        Files.delete(generation.resolve("thumbnail"));
        Files.delete(generation);
        Files.createSymbolicLink(generation, outside);
        failure(() -> storage.read(keys.originalKey(), 2));
        failure(() -> storage.deleteGeneration(keys.originalKey()));
        assertThat(Files.readAllBytes(secret)).containsExactly(7, 8);
    }

    @Test
    void rejectsNonDirectoryRootAndReconcilesOnlyUnreferencedManagedGenerations() throws Exception {
        Path file = Files.write(temporary.resolve("file"), new byte[] {1});
        failure(() -> new ProductImageStorage(file.toString()).stage(processed()));
        var storage = storage();
        var retained = storage.stage(processed());
        var orphan = storage.stage(processed());
        Files.createDirectories(temporary.resolve("managed/operator-files"));
        assertThat(storage.reconcile(Set.of(retained.originalKey()))).isEqualTo(1);
        assertThat(temporary.resolve("managed").resolve(orphan.originalKey()).getParent())
                .doesNotExist();
        assertThat(temporary.resolve("managed/operator-files")).isDirectory();
        assertThat(storage.read(retained.originalKey(), processed().original().length))
                .isNotEmpty();
    }

    @Test
    void readRejectsSymlinkedAncestorEvenWhenGenerationAndLeafAreRegular() throws Exception {
        var storage = storage();
        var image = processed();
        var keys = storage.stage(image);
        Path real = temporary.toRealPath().resolve("managed");
        Path alias = temporary.resolve("read-alias");
        Files.createSymbolicLink(alias, real);
        var aliased = new ProductImageStorage(alias.toString());
        failure(() -> aliased.read(keys.originalKey(), image.original().length));
        assertThat(storage.read(keys.originalKey(), image.original().length)).containsExactly(image.original());
    }

    @Test
    void partialThumbnailWriteFailureRemovesWrittenOriginalAndGeneration() throws Exception {
        var storage = storage();
        var image = processed();
        try (var files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            files.when(() -> Files.write(any(Path.class), any(byte[].class), any(OpenOption[].class)))
                    .thenAnswer(call -> {
                        Path path = call.getArgument(0);
                        if (path.getFileName().toString().equals("thumbnail"))
                            throw new java.io.IOException("Injected thumbnail write failure");
                        return call.callRealMethod();
                    });
            failure(() -> storage.stage(image));
        }
        try (var entries = Files.list(temporary.resolve("managed"))) {
            assertThat(entries.toList()).isEmpty();
        }
    }

    @Test
    void failedPartialStageCleanupRetainsIdentifiableOrphanAndSuppressedCauseForRecovery() throws Exception {
        var storage = storage();
        var image = processed();
        try (var files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            files.when(() -> Files.write(any(Path.class), any(byte[].class), any(OpenOption[].class)))
                    .thenAnswer(call -> {
                        Path path = call.getArgument(0);
                        if (path.getFileName().toString().equals("thumbnail"))
                            throw new java.io.IOException("Injected thumbnail write failure");
                        return call.callRealMethod();
                    });
            files.when(() -> Files.deleteIfExists(any(Path.class)))
                    .thenThrow(new java.io.IOException("Injected cleanup failure"));
            assertThatThrownBy(() -> storage.stage(image))
                    .isInstanceOfSatisfying(
                            ProductImageFailure.class,
                            failure -> assertThat(failure.getCause().getSuppressed())
                                    .hasSize(1));
        }
        assertThat(storage.reconcile(Set.of())).isEqualTo(1);
    }

    private void failure(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(
                        ProductImageFailure.class,
                        failure -> assertThat(failure.reason()).isEqualTo(ProductImageFailure.Reason.STORAGE));
    }
}
