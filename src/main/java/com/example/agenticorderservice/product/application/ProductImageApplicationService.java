package com.example.agenticorderservice.product.application;

import com.example.agenticorderservice.product.application.port.in.ProductImageOperations;
import com.example.agenticorderservice.product.application.port.out.ImageProcessing;
import com.example.agenticorderservice.product.application.port.out.ImageStorage;
import com.example.agenticorderservice.product.application.port.out.ProductImageMetadataStore;
import com.example.agenticorderservice.product.application.port.out.ProductImageTransactions;
import com.example.agenticorderservice.product.domain.ProductImageMetadata;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProductImageApplicationService implements ProductImageOperations {
    private static final Logger LOG = LoggerFactory.getLogger(ProductImageApplicationService.class);
    private final ProductImageMetadataStore images;
    private final ImageProcessing processor;
    private final ImageStorage storage;
    private final ProductImageTransactions transaction;
    private final boolean writesEnabled;

    public ProductImageApplicationService(
            ProductImageMetadataStore images,
            ImageProcessing processor,
            ImageStorage storage,
            ProductImageTransactions transaction,
            @Value("${app.product-images.writes-enabled:false}") boolean writesEnabled) {
        this.images = images;
        this.processor = processor;
        this.storage = storage;
        this.writesEnabled = writesEnabled;
        this.transaction = transaction;
    }

    @Override
    public void upload(UUID productId, byte[] bytes, String declaredType) {
        requireWrites();
        transaction.requireNoAmbientTransaction();
        var processed = processor.process(bytes, declaredType);
        var candidate = storage.stage(processed);
        // Preserve candidates after ANY transaction failure: commit outcome may be uncertain.
        // Offline quiescent reconciliation removes only generations absent from committed metadata.
        String previous = transaction.execute(() -> {
            images.requireProductLocked(productId);
            var existing = images.findByProductId(productId);
            String old = existing.map(ProductImageSnapshot::originalKey).orElse(null);
            var metadata = new ProductImageMetadata(
                    processed.mediaType(),
                    processed.original().length,
                    processed.thumbnail().length,
                    processed.width(),
                    processed.height(),
                    processed.thumbnailWidth(),
                    processed.thumbnailHeight());
            images.save(new ProductImageSnapshot(
                    productId,
                    candidate.originalKey(),
                    candidate.thumbnailKey(),
                    metadata.mediaType(),
                    metadata.originalSize(),
                    metadata.thumbnailSize(),
                    metadata.width(),
                    metadata.height(),
                    metadata.thumbnailWidth(),
                    metadata.thumbnailHeight()));
            return old;
        });
        cleanup(previous, productId);
    }

    @Override
    public ProductImageContent get(UUID productId, boolean thumbnail) {
        transaction.requireNoAmbientTransaction();
        return Objects.requireNonNull(transaction.execute(() -> {
            images.requireProductLocked(productId);
            var image = images.findByProductId(productId)
                    .orElseThrow(() -> new ProductImageFailure(ProductImageFailure.Reason.NOT_FOUND));
            byte[] bytes = storage.read(
                    thumbnail ? image.thumbnailKey() : image.originalKey(),
                    thumbnail ? image.thumbnailSize() : image.originalSize());
            return new ProductImageContent(bytes, thumbnail ? "image/png" : image.mediaType());
        }));
    }

    @Override
    public void delete(UUID productId) {
        requireWrites();
        transaction.requireNoAmbientTransaction();
        String previous = transaction.execute(() -> {
            images.requireProductLocked(productId);
            var image = images.findByProductId(productId);
            if (image.isPresent()) images.delete(productId);
            return image.map(ProductImageSnapshot::originalKey).orElse(null);
        });
        cleanup(previous, productId);
    }

    /** Explicit maintenance operation only; not scheduled and never exposed through HTTP. */
    @Override
    public int reconcileOrphans(boolean quiesced) {
        if (!quiesced) throw new IllegalStateException("All image access across all instances must be quiesced");
        transaction.requireNoAmbientTransaction();
        Set<String> referenced = Objects.requireNonNull(transaction.execute(images::referencedOriginalKeys));
        return storage.reconcile(referenced);
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
}
