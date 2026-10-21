package com.example.agenticorderservice.product.application.port.out;

import com.example.agenticorderservice.product.application.ProcessedImage;
import com.example.agenticorderservice.product.application.StagedImage;
import java.util.Set;

public interface ImageStorage {
    StagedImage stage(ProcessedImage image);

    byte[] read(String key, long expectedSize);

    void deleteGeneration(String originalKey);

    int reconcile(Set<String> referencedOriginalKeys);
}
