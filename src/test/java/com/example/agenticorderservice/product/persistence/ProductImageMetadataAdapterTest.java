package com.example.agenticorderservice.product.persistence;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.product.application.ProductImageSnapshot;
import com.example.agenticorderservice.product.application.ProductNotFoundException;
import com.example.agenticorderservice.product.domain.Product;
import com.example.agenticorderservice.product.domain.ProductImage;
import com.example.agenticorderservice.product.domain.ProductImageMetadata;
import com.example.agenticorderservice.shared.domain.Money;
import java.util.*;
import org.junit.jupiter.api.Test;

class ProductImageMetadataAdapterTest {
    final ProductRepository products = mock(ProductRepository.class);
    final ProductImageRepository images = mock(ProductImageRepository.class);
    final ProductImageMetadataAdapter adapter = new ProductImageMetadataAdapter(products, images);
    final UUID id = UUID.randomUUID();
    final String key = UUID.randomUUID() + "/original";

    ProductImage image() {
        return new ProductImage(
                id,
                key,
                key.replace("original", "thumbnail"),
                new ProductImageMetadata("image/jpeg", 17, 9, 640, 480, 320, 240));
    }

    @Test
    void materializesAllTenFieldsWithoutRevalidatingStoredMetadata() {
        var image = image();
        when(images.findById(id)).thenReturn(Optional.of(image));
        assertThat(adapter.findByProductId(id))
                .contains(new ProductImageSnapshot(
                        id, key, key.replace("original", "thumbnail"), "image/jpeg", 17, 9, 640, 480, 320, 240));
        // A read remains a projection of persisted state, not a new validation operation.
        var hydrated = mock(ProductImage.class);
        when(hydrated.getProductId()).thenReturn(id);
        when(hydrated.getOriginalKey()).thenReturn(key);
        when(images.findById(id)).thenReturn(Optional.of(hydrated));
        assertThat(adapter.findByProductId(id))
                .contains(new ProductImageSnapshot(id, key, null, null, 0, 0, 0, 0, 0, 0));
        when(images.findById(id)).thenReturn(Optional.empty());
        assertThat(adapter.findByProductId(id)).isEmpty();
        verify(images, never()).save(any());
    }

    @Test
    void updatesTheExistingManagedInstanceAndEveryMetadataField() {
        var managed = image();
        when(images.findById(id)).thenReturn(Optional.of(managed));
        String replacementGeneration = UUID.randomUUID().toString();
        var replacement = new ProductImageSnapshot(
                id,
                replacementGeneration + "/original",
                replacementGeneration + "/thumbnail",
                "image/png",
                23,
                11,
                480,
                960,
                160,
                320);
        adapter.save(replacement);
        verify(images).save(same(managed));
        assertThat(adapter.findByProductId(id)).contains(replacement);
    }

    @Test
    void createsAnAssociationAndDeletesOnlyTheExistingManagedInstance() {
        when(images.findById(id)).thenReturn(Optional.empty());
        var snapshot = new ProductImageSnapshot(
                id, key, key.replace("original", "thumbnail"), "image/jpeg", 17, 9, 640, 480, 320, 240);
        adapter.save(snapshot);
        verify(images)
                .save(argThat(image -> image.getProductId().equals(id)
                        && image.getOriginalKey().equals(key)
                        && image.getThumbnailKey().equals(key.replace("original", "thumbnail"))
                        && image.getMediaType().equals("image/jpeg")
                        && image.getOriginalSize() == 17
                        && image.getThumbnailSize() == 9
                        && image.getWidth() == 640
                        && image.getHeight() == 480
                        && image.getThumbnailWidth() == 320
                        && image.getThumbnailHeight() == 240));
        adapter.delete(id);
        verify(images, never()).delete(any());
        var managed = image();
        when(images.findById(id)).thenReturn(Optional.of(managed));
        adapter.delete(id);
        verify(images).delete(same(managed));
    }

    @Test
    void usesTheExistingProductLockAndCollectsOnlyReferencedOriginalKeys() {
        when(products.findLockedById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> adapter.requireProductLocked(id)).isInstanceOf(ProductNotFoundException.class);
        when(products.findLockedById(id)).thenReturn(Optional.of(new Product("Existing", Money.of("1"))));
        adapter.requireProductLocked(id);
        when(images.findAll()).thenReturn(List.of(image(), image()));
        assertThat(adapter.referencedOriginalKeys()).containsExactly(key);
    }
}
