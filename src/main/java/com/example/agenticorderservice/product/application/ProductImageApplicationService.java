package com.example.agenticorderservice.product.application;

import com.example.agenticorderservice.product.domain.ProductImage;
import com.example.agenticorderservice.product.domain.ProductImageMetadata;
import com.example.agenticorderservice.product.persistence.ProductImageRepository;
import com.example.agenticorderservice.product.persistence.ProductRepository;
import com.example.agenticorderservice.product.storage.ProductImageStorage;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ProductImageApplicationService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductImageApplicationService.class);
    private final ProductRepository products;
    private final ProductImageRepository images;
    private final ProductImageProcessor processor;
    private final ProductImageStorage storage;
    private final TransactionTemplate transaction;
    private final boolean writesEnabled;

    public ProductImageApplicationService(
            ProductRepository products,
            ProductImageRepository images,
            ProductImageProcessor processor,
            ProductImageStorage storage,
            PlatformTransactionManager manager,
            @Value("${app.product-images.writes-enabled:false}") boolean writesEnabled) {
        this.products = products;
        this.images = images;
        this.processor = processor;
        this.storage = storage;
        this.writesEnabled = writesEnabled;
        transaction = new TransactionTemplate(manager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    public void upload(UUID productId, byte[] bytes, String declaredType) {
        requireWrites();
        requireNoAmbientTransaction();
        var processed = processor.process(bytes, declaredType);
        var candidate = storage.stage(processed);
        // Preserve candidates after ANY transaction failure: commit outcome may be uncertain.
        // Offline quiescent reconciliation removes only generations absent from committed metadata.
        String previous = transaction.execute(status -> {
            lockProduct(productId);
            var existing = images.findById(productId);
            String old = existing.map(ProductImage::getOriginalKey).orElse(null);
            var metadata = new ProductImageMetadata(
                    processed.mediaType(),
                    processed.original().length,
                    processed.thumbnail().length,
                    processed.width(),
                    processed.height(),
                    processed.thumbnailWidth(),
                    processed.thumbnailHeight());
            var image = existing.orElseGet(
                    () -> new ProductImage(productId, candidate.originalKey(), candidate.thumbnailKey(), metadata));
            if (existing.isPresent()) image.replace(candidate.originalKey(), candidate.thumbnailKey(), metadata);
            images.save(image);
            return old;
        });
        cleanup(previous, productId);
    }

    public ProductImageContent get(UUID productId, boolean thumbnail) {
        requireNoAmbientTransaction();
        return Objects.requireNonNull(transaction.execute(status -> {
            lockProduct(productId);
            var image = images.findById(productId)
                    .orElseThrow(() -> new ProductImageFailure(ProductImageFailure.Reason.NOT_FOUND));
            byte[] bytes = storage.read(
                    thumbnail ? image.getThumbnailKey() : image.getOriginalKey(),
                    thumbnail ? image.getThumbnailSize() : image.getOriginalSize());
            return new ProductImageContent(bytes, thumbnail ? "image/png" : image.getMediaType());
        }));
    }

    public void delete(UUID productId) {
        requireWrites();
        requireNoAmbientTransaction();
        String previous = transaction.execute(status -> {
            lockProduct(productId);
            var image = images.findById(productId);
            image.ifPresent(images::delete);
            return image.map(ProductImage::getOriginalKey).orElse(null);
        });
        cleanup(previous, productId);
    }

    /** Explicit maintenance operation only; not scheduled and never exposed through HTTP. */
    public int reconcileOrphans(boolean quiesced) {
        if (!quiesced) throw new IllegalStateException("All image access across all instances must be quiesced");
        requireNoAmbientTransaction();
        Set<String> referenced = Objects.requireNonNull(transaction.execute(status -> {
            Set<String> keys = new HashSet<>();
            for (var image : images.findAll()) keys.add(image.getOriginalKey());
            return keys;
        }));
        return storage.reconcile(referenced);
    }

    private void lockProduct(UUID productId) {
        products.findLockedById(productId).orElseThrow(ProductNotFoundException::new);
    }

    private void cleanup(String key, UUID productId) {
        if (key == null) return;
        try {
            storage.deleteGeneration(key);
        } catch (ProductImageFailure failure) {
            LOG.warn("Product image cleanup pending productId={} generation={}", productId, key.substring(0, 36));
        }
    }

    private void requireWrites() {
        if (!writesEnabled) throw new ProductImageFailure(ProductImageFailure.Reason.WRITES_DISABLED);
    }

    private void requireNoAmbientTransaction() {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.getResourceMap().isEmpty())
            throw new IllegalStateException("Product images require no ambient transaction or bound resources");
    }
}
