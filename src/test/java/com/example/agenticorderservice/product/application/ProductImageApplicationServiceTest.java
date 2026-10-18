package com.example.agenticorderservice.product.application;

import static com.example.agenticorderservice.product.image.ProductImageFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.product.domain.Product;
import com.example.agenticorderservice.product.domain.ProductImage;
import com.example.agenticorderservice.product.persistence.ProductImageRepository;
import com.example.agenticorderservice.product.persistence.ProductRepository;
import com.example.agenticorderservice.product.storage.ProductImageStorage;
import com.example.agenticorderservice.shared.domain.Money;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.*;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class ProductImageApplicationServiceTest {
    final ProductRepository products = mock(ProductRepository.class);
    final ProductImageRepository images = mock(ProductImageRepository.class);
    final ProductImageStorage storage = mock(ProductImageStorage.class);
    final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    final UUID id = UUID.randomUUID();
    final String generation = UUID.randomUUID().toString();
    final String previous = UUID.randomUUID() + "/original";
    final byte[] bytes = image("png", 640, 320);

    ProductImageApplicationService service(boolean enabled) {
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(products.findLockedById(id)).thenReturn(Optional.of(new Product("Product", Money.of("1"))));
        when(storage.stage(any()))
                .thenReturn(new ProductImageStorage.StagedImage(generation + "/original", generation + "/thumbnail"));
        return new ProductImageApplicationService(
                products, images, new ProductImageProcessor(), storage, manager, enabled);
    }

    ProductImage oldImage() {
        return new ProductImage(
                id,
                previous,
                previous.substring(0, 36) + "/thumbnail",
                "image/png",
                bytes.length,
                4,
                640,
                320,
                320,
                160);
    }

    @Test
    void defaultDisabledWritesRejectWithoutProcessingStorageOrTransactions() {
        var service = service(false);
        assertReason(() -> service.upload(id, bytes, "image/png"), ProductImageFailure.Reason.WRITES_DISABLED);
        assertReason(() -> service.delete(id), ProductImageFailure.Reason.WRITES_DISABLED);
        verifyNoInteractions(storage, images);
        verify(manager, never()).getTransaction(any());
    }

    @Test
    void rejectsAmbientTransactionBeforeStagingAndRequiresQuiescenceForReconciliation() {
        var service = service(true);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            assertThatIllegalStateException().isThrownBy(() -> service.upload(id, bytes, "image/png"));
            assertThatIllegalStateException().isThrownBy(() -> service.get(id, false));
            assertThatIllegalStateException().isThrownBy(() -> service.delete(id));
        } finally {
            TransactionSynchronizationManager.clear();
        }
        assertThatIllegalStateException().isThrownBy(() -> service.reconcileOrphans(false));
        verifyNoInteractions(storage, images);
    }

    @Test
    void rejectedReplacementDoesNotStageChangeOrDeleteExistingFiles() {
        var service = service(true);
        assertReason(() -> service.upload(id, new byte[] {1}, "image/png"), ProductImageFailure.Reason.INVALID);
        verifyNoInteractions(storage, images);
        verify(manager, never()).getTransaction(any());
    }

    @Test
    void replacesMetadataAndCleansOldGenerationOnlyAfterCommit() {
        var service = service(true);
        var old = oldImage();
        when(images.findById(id)).thenReturn(Optional.of(old));
        service.upload(id, bytes, "image/png");
        assertThat(old.getOriginalKey()).isEqualTo(generation + "/original");
        assertThat(old.getThumbnailKey()).isEqualTo(generation + "/thumbnail");
        assertThat(old.getOriginalSize()).isEqualTo(bytes.length);
        var order = inOrder(products, images, manager, storage);
        order.verify(storage).stage(any());
        order.verify(manager).getTransaction(any());
        order.verify(products).findLockedById(id);
        order.verify(images).findById(id);
        order.verify(images).save(old);
        order.verify(manager).commit(any());
        order.verify(storage).deleteGeneration(previous);
    }

    @Test
    void failedSaveAndUncertainCommitRetainCandidateAndPreviousFiles() {
        var service = service(true);
        when(images.findById(id)).thenReturn(Optional.of(oldImage()));
        when(images.save(any())).thenThrow(new IllegalStateException("database failure"));
        assertThatIllegalStateException().isThrownBy(() -> service.upload(id, bytes, "image/png"));
        verify(storage, never()).deleteGeneration(any());
        verify(manager).rollback(any());
        reset(images, manager);
        var uncertain = service(true);
        when(images.findById(id)).thenReturn(Optional.of(oldImage()));
        doThrow(new TransactionSystemException("commit outcome unknown"))
                .when(manager)
                .commit(any());
        assertThatThrownBy(() -> uncertain.upload(id, bytes, "image/png"))
                .isInstanceOf(TransactionSystemException.class);
        verify(storage, never()).deleteGeneration(any());
    }

    @Test
    void cleanupFailureDoesNotMisreportCommittedReplacementAsFailed() {
        var service = service(true);
        when(images.findById(id)).thenReturn(Optional.of(oldImage()));
        doThrow(new ProductImageFailure(ProductImageFailure.Reason.STORAGE))
                .when(storage)
                .deleteGeneration(previous);
        assertThatCode(() -> service.upload(id, bytes, "image/png")).doesNotThrowAnyException();
        verify(manager).commit(any());
    }

    @Test
    void missingProductImageAndIdempotentDeletionHaveDistinctResults() {
        var service = service(true);
        when(products.findLockedById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(id, false)).isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ProductNotFoundException.class);
        when(products.findLockedById(id)).thenReturn(Optional.of(new Product("Valid", Money.of("1"))));
        when(images.findById(id)).thenReturn(Optional.empty());
        assertReason(() -> service.get(id, false), ProductImageFailure.Reason.NOT_FOUND);
        service.delete(id);
        service.delete(id);
        verify(images, never()).delete(any());
        verify(storage, never()).deleteGeneration(any());
    }

    @Test
    void readUsesVariantMetadataAndQuiescentReconcilePreservesReferencedGenerations() {
        var service = service(true);
        var image = oldImage();
        when(images.findById(id)).thenReturn(Optional.of(image));
        when(storage.read(previous, bytes.length)).thenReturn(bytes);
        when(storage.read(image.getThumbnailKey(), 4)).thenReturn(new byte[] {1, 2, 3, 4});
        assertThat(service.get(id, false).bytes()).containsExactly(bytes);
        assertThat(service.get(id, true).mediaType()).isEqualTo("image/png");
        when(images.findAll()).thenReturn(List.of(image));
        when(storage.reconcile(Set.of(previous))).thenReturn(2);
        assertThat(service.reconcileOrphans(true)).isEqualTo(2);
    }

    @Test
    void deletionRemovesAssociationBeforeCommitAndCleansFilesOnlyAfterCommit() {
        var service = service(true);
        var image = oldImage();
        when(images.findById(id)).thenReturn(Optional.of(image));
        service.delete(id);
        var order = inOrder(products, images, manager, storage);
        order.verify(manager)
                .getTransaction(argThat(definition ->
                        definition.getIsolationLevel() == TransactionDefinition.ISOLATION_READ_COMMITTED));
        order.verify(products).findLockedById(id);
        order.verify(images).findById(id);
        order.verify(images).delete(image);
        order.verify(manager).commit(any());
        order.verify(storage).deleteGeneration(previous);
    }

    @Test
    void jpegOriginalAndPngThumbnailUseTheirOwnMediaTypesAndLengths() {
        var service = service(true);
        var image = new ProductImage(
                id, previous, previous.substring(0, 36) + "/thumbnail", "image/jpeg", 7, 3, 640, 320, 320, 160);
        when(images.findById(id)).thenReturn(Optional.of(image));
        when(storage.read(previous, 7)).thenReturn(new byte[] {1, 2, 3, 4, 5, 6, 7});
        when(storage.read(image.getThumbnailKey(), 3)).thenReturn(new byte[] {8, 9, 10});
        assertThat(service.get(id, false).mediaType()).isEqualTo("image/jpeg");
        var thumbnail = service.get(id, true);
        assertThat(thumbnail.mediaType()).isEqualTo("image/png");
        assertThat(thumbnail.bytes()).containsExactly(8, 9, 10);
    }

    @Test
    void rejectsSynchronizationAndBoundResourceEntryIncludingMaintenance() {
        var service = service(true);
        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThatIllegalStateException().isThrownBy(() -> service.reconcileOrphans(true));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
        Object resource = new Object();
        TransactionSynchronizationManager.bindResource(resource, new Object());
        try {
            assertThatIllegalStateException().isThrownBy(() -> service.upload(id, bytes, "image/png"));
            assertThatIllegalStateException().isThrownBy(() -> service.reconcileOrphans(true));
        } finally {
            TransactionSynchronizationManager.unbindResource(resource);
        }
        verifyNoInteractions(images, storage);
    }

    private void assertReason(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable action, ProductImageFailure.Reason reason) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(
                        ProductImageFailure.class,
                        failure -> assertThat(failure.reason()).isEqualTo(reason));
    }
}
