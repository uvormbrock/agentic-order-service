package com.example.agenticorderservice.customer.api;

import com.example.agenticorderservice.customer.application.CustomerApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Customers")
@RestController
@RequestMapping("/customers")
public class CustomerController {
    private final CustomerApplicationService service;

    public CustomerController(CustomerApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(
            summary = "Create a customer",
            description =
                    "Creates an active customer; email is normalized and unique across active and inactive customers.")
    @ApiResponse(responseCode = "201", description = "Created; Location identifies the new resource")
    public ResponseEntity<CustomerResponse> create(@RequestBody CustomerDetailsRequest input) {
        var response = CustomerResponse.from(service.create(input.name(), input.email()));
        return ResponseEntity.created(URI.create("/customers/" + response.id()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @GetMapping
    @Operation(
            summary = "List customers",
            description = "Includes active and inactive customers; no pagination contract is implemented.")
    public ResponseEntity<List<CustomerResponse>> list() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.list().stream().map(CustomerResponse::from).toList());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get a customer",
            description = "Returns the current customer details, including inactive records.")
    public ResponseEntity<CustomerResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(CustomerResponse.from(service.get(id)));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update customer details",
            description =
                    "Updates name/email without reactivation; confirmed optimistic conflicts return 409 without automatic replay.")
    public ResponseEntity<CustomerResponse> update(@PathVariable UUID id, @RequestBody CustomerDetailsRequest input) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(CustomerResponse.from(service.update(id, input.name(), input.email())));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete or deactivate a customer",
            description =
                    "Customer-first locking: physically deletes an unreferenced customer or deactivates a referenced one. Returns 204 without a body.")
    @ApiResponse(responseCode = "204", description = "Customer removed or deactivated; no response body")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Deactivate a customer",
            description =
                    "Only active=false is supported. Repeating a completed deactivation returns the unchanged representation; no reactivation.")
    public ResponseEntity<CustomerResponse> deactivate(
            @PathVariable UUID id, @RequestBody DeactivateCustomerRequest input) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(CustomerResponse.from(service.deactivate(id)));
    }
}
