package com.example.agenticorderservice.product.api;

import com.example.agenticorderservice.product.application.*;
import com.example.agenticorderservice.shared.api.ApiError;
import com.example.agenticorderservice.shared.api.ApiInputException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Products")
@RestController
@RequestMapping("/products/{id}/image")
public class ProductImageController {
    private final ProductImageApplicationService service;

    public ProductImageController(ProductImageApplicationService service) {
        this.service = service;
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload or replace a product image",
            description =
                    "JPEG/PNG file, at most 10 MiB and 40 million pixels. Original bytes preserved; PNG thumbnail fits 320x320 without upscaling. Writes require explicit trusted-deployment opt-in.")
    @ApiResponse(responseCode = "204", description = "Original and thumbnail association committed")
    @ApiResponse(responseCode = "403", description = "Image writes disabled")
    @ApiResponse(responseCode = "400", description = "Missing, empty, corrupt or invalid image")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @ApiResponse(responseCode = "413", description = "Image exceeds upload limit")
    @ApiResponse(responseCode = "415", description = "Unsupported image or multipart media type")
    public ResponseEntity<Void> upload(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        if (file.getSize() > ProductImageProcessor.MAX_BYTES)
            throw new ProductImageFailure(ProductImageFailure.Reason.TOO_LARGE);
        try (var input = file.getInputStream()) {
            byte[] bytes = input.readNBytes(ProductImageProcessor.MAX_BYTES + 1);
            service.upload(id, bytes, file.getContentType());
        } catch (IOException failure) {
            throw new ProductImageFailure(ProductImageFailure.Reason.STORAGE, failure);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(
            summary = "Retrieve original product image",
            description = "Returns original JPEG/PNG bytes; inactive products remain readable.")
    @ApiResponse(responseCode = "404", description = "Product or image not found")
    public ResponseEntity<byte[]> original(
            @PathVariable UUID id, @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept) {
        return response(service.get(id, false), accept);
    }

    @GetMapping("/thumbnail")
    @Operation(
            summary = "Retrieve product image thumbnail",
            description = "Returns generated aspect-ratio-preserving PNG thumbnail.")
    @ApiResponse(responseCode = "404", description = "Product or image not found")
    public ResponseEntity<byte[]> thumbnail(
            @PathVariable UUID id, @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept) {
        return response(service.get(id, true), accept);
    }

    @DeleteMapping
    @Operation(
            summary = "Delete product image",
            description =
                    "Idempotent when product exists without an image. Requires image-write opt-in; obsolete generation cleanup is recoverable maintenance.")
    @ApiResponse(responseCode = "204", description = "Image association removed")
    @ApiResponse(responseCode = "403", description = "Image writes disabled")
    @ApiResponse(responseCode = "404", description = "Product not found")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<byte[]> response(ProductImageContent image, String accept) {
        MediaType actual = MediaType.parseMediaType(image.mediaType());
        if (accept != null) {
            var matches = MediaType.parseMediaTypes(accept).stream()
                    .filter(type -> type.isCompatibleWith(actual))
                    .sorted(Comparator.comparingInt(
                                    (MediaType type) -> type.isWildcardType() ? 0 : type.isWildcardSubtype() ? 1 : 2)
                            .reversed())
                    .toList();
            if (matches.isEmpty() || matches.getFirst().getQualityValue() <= 0)
                throw new ApiInputException(ApiError.Code.NOT_ACCEPTABLE);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.mediaType()))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(image.bytes());
    }
}
