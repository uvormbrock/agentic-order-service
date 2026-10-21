package com.example.agenticorderservice.product.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.agenticorderservice.product.application.port.out.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class ProductImagePortsTest {
    final ProductImageMetadataStore metadata = mock(ProductImageMetadataStore.class);
    final ImageProcessing processor = mock(ImageProcessing.class);
    final ImageStorage storage = mock(ImageStorage.class);
    final ProductImageTransactions transactions = mock(ProductImageTransactions.class);
    final UUID id = UUID.randomUUID();
    final ProductImageApplicationService service =
            new ProductImageApplicationService(metadata, processor, storage, transactions, true);

    @Test
    void uploadStagesBeforeMetadataAttemptAndCleansOnlyAfterSuccessfulCompletion() {
        byte[] original = {1, 2, 3};
        var processed = new ProcessedImage(original, new byte[] {4, 5}, "image/jpeg", 640, 480, 320, 240);
        var previous =
                new ProductImageSnapshot(id, "old/original", "old/thumbnail", "image/png", 4, 2, 480, 960, 160, 320);
        when(processor.process(original, "image/jpeg")).thenReturn(processed);
        when(storage.stage(processed)).thenReturn(new StagedImage("new/original", "new/thumbnail"));
        when(metadata.findByProductId(id)).thenReturn(Optional.of(previous));
        when(transactions.execute(any())).thenAnswer(call -> ((Supplier<?>) call.getArgument(0)).get());
        service.upload(id, original, "image/jpeg");
        var order = inOrder(transactions, processor, storage, metadata);
        order.verify(transactions).requireNoAmbientTransaction();
        order.verify(processor).process(original, "image/jpeg");
        order.verify(storage).stage(processed);
        order.verify(transactions).execute(any());
        order.verify(metadata).requireProductLocked(id);
        order.verify(metadata).findByProductId(id);
        order.verify(metadata)
                .save(new ProductImageSnapshot(
                        id, "new/original", "new/thumbnail", "image/jpeg", 3, 2, 640, 480, 320, 240));
        order.verify(storage).deleteGeneration("old/original");
    }

    @Test
    void failedMetadataReadPreventsReconciliationAndContentReadsRemainAvailableWithWritesDisabled() {
        var failure = new IllegalStateException("metadata unavailable");
        when(transactions.execute(any())).thenAnswer(call -> ((Supplier<?>) call.getArgument(0)).get());
        when(metadata.referencedOriginalKeys()).thenThrow(failure);
        assertThatThrownBy(() -> service.reconcileOrphans(true)).isSameAs(failure);
        verify(storage, never()).reconcile(any());
        var readOnly = new ProductImageApplicationService(metadata, processor, storage, transactions, false);
        var snapshot =
                new ProductImageSnapshot(id, "old/original", "old/thumbnail", "image/jpeg", 3, 2, 640, 480, 320, 240);
        when(metadata.findByProductId(id)).thenReturn(Optional.of(snapshot));
        when(storage.read("old/original", 3)).thenReturn(new byte[] {1, 2, 3});
        assertThat(readOnly.get(id, false)).isEqualTo(new ProductImageContent(new byte[] {1, 2, 3}, "image/jpeg"));
        verifyNoInteractions(processor);
    }
}
