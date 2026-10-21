package com.example.agenticorderservice.product.application.port.out;

import com.example.agenticorderservice.product.application.ProcessedImage;

public interface ImageProcessing {
    ProcessedImage process(byte[] bytes, String declaredType);
}
