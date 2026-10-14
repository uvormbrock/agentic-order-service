package com.example.agenticorderservice.customer.api;
import com.example.agenticorderservice.customer.application.CustomerApplicationService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
public class CustomerController {
    private final CustomerApplicationService service;
    public CustomerController(CustomerApplicationService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<CustomerResponse> create(@RequestBody CustomerDetailsRequest input) {
        var response = CustomerResponse.from(service.create(input.name(), input.email()));
        return ResponseEntity.created(URI.create("/customers/" + response.id())).contentType(MediaType.APPLICATION_JSON).body(response);
    }
    @GetMapping
    public ResponseEntity<List<CustomerResponse>> list() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.list().stream().map(CustomerResponse::from).toList());
    }
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(CustomerResponse.from(service.get(id)));
    }
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> update(@PathVariable UUID id, @RequestBody CustomerDetailsRequest input) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(CustomerResponse.from(service.update(id, input.name(), input.email())));
    }
    @PatchMapping("/{id}")
    public ResponseEntity<CustomerResponse> deactivate(@PathVariable UUID id, @RequestBody DeactivateCustomerRequest input) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(CustomerResponse.from(service.deactivate(id)));
    }
}
