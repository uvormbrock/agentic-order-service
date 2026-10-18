package com.example.agenticorderservice.product.application;

import static com.example.agenticorderservice.product.image.ProductImageFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.PostgreSqlTestConfiguration;
import com.example.agenticorderservice.product.domain.ProductImage;
import com.example.agenticorderservice.product.storage.ProductImageStorage;
import jakarta.persistence.EntityManagerFactory;
import java.io.ByteArrayInputStream;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest(properties = "app.product-images.writes-enabled=true")
@ActiveProfiles("integration-test")
@Import(PostgreSqlTestConfiguration.class)
class ProductImageLifecycleIT {
    static final Path ROOT = root();

    static Path root() {
        try {
            return Files.createTempDirectory("CH-002-images-it-").toRealPath();
        } catch (Exception failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.product-images.storage-directory", ROOT::toString);
    }

    @Autowired
    ProductImageApplicationService images;

    @Autowired
    ProductApplicationService products;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager manager;

    @Autowired
    EntityManagerFactory factory;

    @jakarta.persistence.PersistenceContext
    jakarta.persistence.EntityManager entityManager;

    @MockitoSpyBean
    ProductImageStorage storage;

    @MockitoSpyBean
    com.example.agenticorderservice.product.persistence.ProductImageRepository metadata;

    UUID product(boolean active) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into products(id,name,price,active,version) values (?,'Image fixture',25,?,0)", id, active);
        return id;
    }

    String key(UUID id) {
        return jdbc.queryForObject("select original_key from product_images where product_id=?", String.class, id);
    }

    @Test
    void imagelessInactiveAndCompleteLifecyclePreserveExistingProductContracts() throws Exception {
        UUID id = product(false);
        assertThat(products.get(id).active()).isFalse();
        assertThatThrownBy(() -> images.get(id, false))
                .isInstanceOfSatisfying(
                        ProductImageFailure.class,
                        error -> assertThat(error.reason()).isEqualTo(ProductImageFailure.Reason.NOT_FOUND));
        images.delete(id);
        byte[] original = image("jpg", 960, 480);
        images.upload(id, original, "image/jpeg");
        String first = key(id);
        assertThat(images.get(id, false).bytes()).containsExactly(original);
        var thumbnail =
                ImageIO.read(new ByteArrayInputStream(images.get(id, true).bytes()));
        assertThat(thumbnail.getWidth()).isEqualTo(320);
        assertThat(thumbnail.getHeight()).isEqualTo(160);
        assertThat(products.get(id).active()).isFalse();
        assertThatThrownBy(() -> images.upload(id, new byte[] {3}, "image/png"))
                .isInstanceOf(ProductImageFailure.class);
        assertThat(key(id)).isEqualTo(first);
        assertThat(images.get(id, false).bytes()).containsExactly(original);
        byte[] replacement = image("png", 20, 30);
        images.upload(id, replacement, "image/png");
        assertThat(key(id)).isNotEqualTo(first);
        assertThat(ROOT.resolve(first).getParent()).doesNotExist();
        assertThat(images.get(id, false).bytes()).containsExactly(replacement);
        String last = key(id);
        images.delete(id);
        images.delete(id);
        assertThat(jdbc.queryForObject("select count(*) from product_images where product_id=?", Integer.class, id))
                .isZero();
        assertThat(ROOT.resolve(last).getParent()).doesNotExist();
        assertThat(products.get(id).active()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void roundTripsAllTenCommittedMetadataFieldsAcrossBothImageFormatOrders(boolean pngFirst) throws Exception {
        UUID productId = product(false);
        Path previousGeneration = null;
        for (String format : pngFirst ? new String[] {"png", "jpg"} : new String[] {"jpg", "png"}) {
            boolean jpeg = format.equals("jpg");
            int width = jpeg ? 960 : 20;
            int height = jpeg ? 480 : 30;
            int thumbnailWidth = jpeg ? 320 : 20;
            int thumbnailHeight = jpeg ? 160 : 30;
            String mediaType = jpeg ? "image/jpeg" : "image/png";
            byte[] original = image(format, width, height);
            Set<Path> before;
            try (var generations = Files.list(ROOT)) {
                before = new HashSet<>(generations.toList());
            }
            images.upload(productId, original, mediaType);
            Set<Path> created;
            try (var generations = Files.list(ROOT)) {
                created = new HashSet<>(generations.toList());
            }
            created.removeAll(before);
            assertThat(created)
                    .as("one new physical generation per committed upload")
                    .hasSize(1);
            Path generation = created.iterator().next();
            String generationId = generation.getFileName().toString();
            assertThat(UUID.fromString(generationId).toString()).isEqualTo(generationId);
            String originalKey = generationId + "/original";
            String thumbnailKey = generationId + "/thumbnail";
            Path originalFile = generation.resolve("original");
            Path thumbnailFile = generation.resolve("thumbnail");
            assertThat(Files.readAllBytes(originalFile)).containsExactly(original);
            byte[] thumbnail = Files.readAllBytes(thumbnailFile);
            assertThat(thumbnail).startsWith(new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
            var decoded = ImageIO.read(new ByteArrayInputStream(thumbnail));
            assertThat(decoded).isNotNull();
            assertThat(decoded.getWidth()).isEqualTo(thumbnailWidth);
            assertThat(decoded.getHeight()).isEqualTo(thumbnailHeight);
            long originalSize = original.length;
            long thumbnailSize = Files.size(thumbnailFile);
            assertThat(thumbnailSize).isEqualTo(thumbnail.length);

            // Upload returned after commit; a separate EntityManager and transaction cannot reuse its entity.
            PostgreSqlTestConfiguration.inTransaction(factory, reloading -> {
                reloading.clear();
                var persisted = reloading.find(ProductImage.class, productId);
                assertThat(persisted).isNotNull();
                assertThat(persisted.getProductId()).isEqualTo(productId);
                assertThat(persisted.getOriginalKey()).isEqualTo(originalKey);
                assertThat(persisted.getThumbnailKey()).isEqualTo(thumbnailKey);
                assertThat(persisted.getMediaType()).isEqualTo(mediaType);
                assertThat(persisted.getOriginalSize()).isEqualTo(originalSize);
                assertThat(persisted.getThumbnailSize()).isEqualTo(thumbnailSize);
                assertThat(persisted.getWidth()).isEqualTo(width);
                assertThat(persisted.getHeight()).isEqualTo(height);
                assertThat(persisted.getThumbnailWidth()).isEqualTo(thumbnailWidth);
                assertThat(persisted.getThumbnailHeight()).isEqualTo(thumbnailHeight);
                reloading.clear();
                return null;
            });
            Map<String, Object> expected = Map.of(
                    "product_id",
                    productId,
                    "original_key",
                    originalKey,
                    "thumbnail_key",
                    thumbnailKey,
                    "media_type",
                    mediaType,
                    "original_size",
                    originalSize,
                    "thumbnail_size",
                    thumbnailSize,
                    "width",
                    width,
                    "height",
                    height,
                    "thumbnail_width",
                    thumbnailWidth,
                    "thumbnail_height",
                    thumbnailHeight);
            assertThat(jdbc.queryForMap(
                            "select product_id,original_key,thumbnail_key,media_type,original_size,thumbnail_size,width,height,thumbnail_width,thumbnail_height from product_images where product_id=?",
                            productId))
                    .containsExactlyInAnyOrderEntriesOf(expected);
            assertThat(jdbc.queryForObject(
                            "select count(*) from product_images where product_id=?", Integer.class, productId))
                    .isEqualTo(1);
            assertThat(jdbc.queryForObject("select active from products where id=?", Boolean.class, productId))
                    .isFalse();
            if (previousGeneration != null) {
                assertThat(generation).isNotEqualTo(previousGeneration);
                assertThat(previousGeneration).doesNotExist();
            }
            previousGeneration = generation;
        }
    }

    @Test
    void databaseRejectsDuplicateAssociationAndDanglingProductForeignKey() {
        UUID id = product(true);
        images.upload(id, image("png", 4, 4), "image/png");
        String columns =
                "original_key,thumbnail_key,media_type,original_size,thumbnail_size,width,height,thumbnail_width,thumbnail_height";
        assertThatThrownBy(() -> jdbc.update(
                        "insert into product_images select product_id," + columns
                                + " from product_images where product_id=?",
                        id))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        String orphan = UUID.randomUUID().toString();
        assertThatThrownBy(() -> jdbc.update(
                        "insert into product_images select ?,?,?,media_type,original_size,thumbnail_size,width,height,thumbnail_width,thumbnail_height from product_images where product_id=?",
                        UUID.randomUUID(),
                        orphan + "/original",
                        orphan + "/thumbnail",
                        id))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                .hasMessageContaining("fk_product_images_product");
        assertThatThrownBy(() -> jdbc.update("delete from products where id=?", id))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void concurrentFirstUploadsSerializeIntoOneAssociationWithOneLiveGeneration() throws Exception {
        UUID id = product(true);
        byte[] first = image("png", 640, 320), second = image("jpg", 320, 640);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> {
                await(start);
                images.upload(id, first, "image/png");
            });
            var b = pool.submit(() -> {
                await(start);
                images.upload(id, second, "image/jpeg");
            });
            start.countDown();
            a.get(20, TimeUnit.SECONDS);
            b.get(20, TimeUnit.SECONDS);
        }
        assertThat(jdbc.queryForObject("select count(*) from product_images where product_id=?", Integer.class, id))
                .isEqualTo(1);
        byte[] actual = images.get(id, false).bytes();
        assertThat(Arrays.equals(actual, first) || Arrays.equals(actual, second))
                .isTrue();
        assertThat(ROOT.resolve(key(id))).isRegularFile();
    }

    @Test
    void readHoldsProductLockUntilBytesAreMaterializedBeforeConcurrentDelete() throws Exception {
        UUID id = product(true);
        byte[] bytes = image("png", 640, 320);
        images.upload(id, bytes, "image/png");
        String key = key(id);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        doAnswer(call -> {
                    entered.countDown();
                    await(release);
                    return call.callRealMethod();
                })
                .when(storage)
                .read(eq(key), anyLong());
        try (var pool = Executors.newFixedThreadPool(2)) {
            var read = pool.submit(() -> images.get(id, false));
            assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();
            var deletion = pool.submit(() -> images.delete(id));
            try {
                awaitLockWaiter();
                assertThat(deletion.isDone()).isFalse();
                assertThat(ROOT.resolve(key)).isRegularFile();
            } finally {
                release.countDown();
            }
            assertThat(read.get(20, TimeUnit.SECONDS).bytes()).containsExactly(bytes);
            deletion.get(20, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            reset(storage);
        }
        assertThat(ROOT.resolve(key).getParent()).doesNotExist();
    }

    @Test
    void realTransactionRollbackPreservesPreviousAssociationAndGenerationAfterReplacementFailure() {
        UUID id = product(true);
        byte[] original = image("png", 640, 320);
        images.upload(id, original, "image/png");
        String previous = key(id);
        doAnswer(call -> {
                    entityManager.flush();
                    throw new IllegalStateException("Injected failure after metadata replacement");
                })
                .when(metadata)
                .save(any());
        try {
            assertThatIllegalStateException().isThrownBy(() -> images.upload(id, image("jpg", 20, 30), "image/jpeg"));
        } finally {
            reset(metadata);
        }
        assertThat(key(id)).isEqualTo(previous);
        assertThat(images.get(id, false).bytes()).containsExactly(original);
        assertThat(ROOT.resolve(previous)).isRegularFile();
        assertThat(images.reconcileOrphans(true)).isGreaterThanOrEqualTo(1);
        assertThat(images.get(id, false).bytes()).containsExactly(original);
    }

    @Test
    void databaseFailureLeavesOnlyIdentifiableOrphansForExplicitQuiescentRecovery() {
        UUID missing = UUID.randomUUID();
        assertThatThrownBy(() -> images.upload(missing, image("png", 2, 2), "image/png"))
                .isInstanceOf(ProductNotFoundException.class);
        assertThatIllegalStateException().isThrownBy(() -> images.reconcileOrphans(false));
        assertThat(images.reconcileOrphans(true)).isGreaterThanOrEqualTo(1);
        assertThatThrownBy(() -> images.get(missing, false)).isInstanceOf(ProductNotFoundException.class);
    }

    void awaitLockWaiter() {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        do {
            Integer waiting = jdbc.queryForObject(
                    "select count(*) from pg_stat_activity where datname=current_database() and wait_event_type='Lock' and query like '%products%'",
                    Integer.class);
            if (waiting != null && waiting > 0) return;
            java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5));
        } while (System.nanoTime() < deadline);
        fail("No PostgreSQL product lock waiter observed");
    }

    static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(failure);
        }
    }
}
