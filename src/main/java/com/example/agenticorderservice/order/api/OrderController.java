package com.example.agenticorderservice.order.api;
import com.example.agenticorderservice.order.application.*;
import java.net.URI;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderPlacementCoordinator placement;
    private final OrderQueryService queries;
    public OrderController(OrderPlacementCoordinator placement, OrderQueryService queries) { this.placement = placement; this.queries = queries; }
    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestBody CreateOrderRequest input) {
        var command = new PlaceOrderCommand(input.customerId(), input.items().stream()
                .map(item -> new PlaceOrderCommand.Line(item.productId(), item.quantity())).toList());
        var result = OrderResponse.from(placement.place(command));
        return ResponseEntity.created(URI.create("/orders/" + result.id())).contentType(MediaType.APPLICATION_JSON).body(result);
    }
    @GetMapping
    public ResponseEntity<List<OrderResponse>> list() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(queries.list().stream().map(OrderResponse::from).toList());
    }
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(OrderResponse.from(queries.get(id)));
    }
}
