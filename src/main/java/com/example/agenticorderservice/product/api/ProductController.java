package com.example.agenticorderservice.product.api;

import com.example.agenticorderservice.product.application.ProductApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Products")
@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductApplicationService service;

    public ProductController(ProductApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(
            summary = "Create a product",
            description = "Creates an active product with a server-generated ID and validated EUR gross price.")
    @ApiResponse(responseCode = "201", description = "Created; Location identifies the new resource")
    public ResponseEntity<ProductResponse> create(@RequestBody ProductDetailsRequest input) {
        var response = ProductResponse.from(service.create(input.name(), input.price()));
        return ResponseEntity.created(URI.create("/products/" + response.id()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @GetMapping
    @Operation(
            summary = "List products",
            description = "Includes active and inactive products; no pagination contract is implemented.")
    public ResponseEntity<List<ProductResponse>> list() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.list().stream().map(ProductResponse::from).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product", description = "Returns current catalog data, including inactive products.")
    public ResponseEntity<ProductResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(ProductResponse.from(service.get(id)));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update product details",
            description =
                    "Updates current name/price without changing historical order snapshots or reactivating the product. Confirmed optimistic conflicts return 409.")
    public ResponseEntity<ProductResponse> update(@PathVariable UUID id, @RequestBody ProductDetailsRequest input) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ProductResponse.from(service.update(id, input.name(), input.price())));
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Deactivate a product",
            description =
                    "Only active=false is supported. Repeating a completed deactivation returns the unchanged representation; new orders cannot use inactive products.")
    public ResponseEntity<ProductResponse> deactivate(
            @PathVariable UUID id, @RequestBody DeactivateProductRequest input) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ProductResponse.from(service.deactivate(id)));
    }
}
