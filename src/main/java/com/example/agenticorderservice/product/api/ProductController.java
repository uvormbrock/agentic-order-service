package com.example.agenticorderservice.product.api;

import com.example.agenticorderservice.product.application.ProductApplicationService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductApplicationService service;

    public ProductController(ProductApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@RequestBody ProductDetailsRequest input) {
        var response = ProductResponse.from(service.create(input.name(), input.price()));
        return ResponseEntity.created(URI.create("/products/" + response.id()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> list() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.list().stream().map(ProductResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(ProductResponse.from(service.get(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable UUID id, @RequestBody ProductDetailsRequest input) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ProductResponse.from(service.update(id, input.name(), input.price())));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductResponse> deactivate(
            @PathVariable UUID id, @RequestBody DeactivateProductRequest input) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ProductResponse.from(service.deactivate(id)));
    }
}
