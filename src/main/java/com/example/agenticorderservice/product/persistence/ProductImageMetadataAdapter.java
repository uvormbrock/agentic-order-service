package com.example.agenticorderservice.product.persistence;

import com.example.agenticorderservice.product.application.ProductImageSnapshot;
import com.example.agenticorderservice.product.application.ProductNotFoundException;
import com.example.agenticorderservice.product.application.port.out.ProductImageMetadataStore;
import com.example.agenticorderservice.product.domain.ProductImage;
import com.example.agenticorderservice.product.domain.ProductImageMetadata;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProductImageMetadataAdapter implements ProductImageMetadataStore {
    private final ProductRepository products;
    private final ProductImageRepository images;

    public ProductImageMetadataAdapter(ProductRepository products, ProductImageRepository images) {
        this.products = products;
        this.images = images;
    }

    @Override
    public void requireProductLocked(UUID productId) {
        products.findLockedById(productId).orElseThrow(ProductNotFoundException::new);
    }

    @Override
    public Optional<ProductImageSnapshot> findByProductId(UUID productId) {
        return images.findById(productId)
                .map(image -> new ProductImageSnapshot(
                        image.getProductId(),
                        image.getOriginalKey(),
                        image.getThumbnailKey(),
                        image.getMediaType(),
                        image.getOriginalSize(),
                        image.getThumbnailSize(),
                        image.getWidth(),
                        image.getHeight(),
                        image.getThumbnailWidth(),
                        image.getThumbnailHeight()));
    }

    @Override
    public void save(ProductImageSnapshot snapshot) {
        var metadata = new ProductImageMetadata(
                snapshot.mediaType(),
                snapshot.originalSize(),
                snapshot.thumbnailSize(),
                snapshot.width(),
                snapshot.height(),
                snapshot.thumbnailWidth(),
                snapshot.thumbnailHeight());
        var existing = images.findById(snapshot.productId());
        var image = existing.orElseGet(() ->
                new ProductImage(snapshot.productId(), snapshot.originalKey(), snapshot.thumbnailKey(), metadata));
        if (existing.isPresent()) image.replace(snapshot.originalKey(), snapshot.thumbnailKey(), metadata);
        images.save(image);
    }

    @Override
    public void delete(UUID productId) {
        images.findById(productId).ifPresent(images::delete);
    }

    @Override
    public Set<String> referencedOriginalKeys() {
        Set<String> keys = new HashSet<>();
        for (var image : images.findAll()) keys.add(image.getOriginalKey());
        return keys;
    }
}
