package com.example.agenticorderservice.order.api;

import com.example.agenticorderservice.order.application.*;
import com.example.agenticorderservice.order.application.port.in.OrderPlacementOperations;
import com.example.agenticorderservice.order.application.port.in.OrderQueries;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Orders")
@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderPlacementOperations placement;
    private final OrderQueries queries;

    public OrderController(OrderPlacementOperations placement, OrderQueries queries) {
        this.placement = placement;
        this.queries = queries;
    }

    @PostMapping
    @Operation(
            summary = "Place an order",
            description =
                    "Accepts only customerId and productId/quantity lines. Consolidates duplicate products (at most 10,000 per product), locks Customer then Products deterministically and calculates prices on the server. Subtotal >= EUR 100.00 receives a 10% HALF_UP discount. At most three independent attempts follow confirmed rollback; uncertain commits are never replayed.")
    @ApiResponse(responseCode = "201", description = "Created; Location identifies the new resource")
    public ResponseEntity<OrderResponse> create(@RequestBody CreateOrderRequest input) {
        var command = new PlaceOrderCommand(
                input.customerId(),
                input.items().stream()
                        .map(item -> new PlaceOrderCommand.Line(item.productId(), item.quantity()))
                        .toList());
        var result = OrderResponse.from(placement.place(command));
        return ResponseEntity.created(URI.create("/orders/" + result.id()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    @GetMapping
    @Operation(
            summary = "List orders",
            description =
                    "Returns historical product names, prices and totals with current customer details, including inactive customers.")
    public ResponseEntity<List<OrderResponse>> list() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(queries.list().stream().map(OrderResponse::from).toList());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get an order",
            description =
                    "Returns historical item snapshots and calculated amounts with current customer details. Later catalog changes do not alter stored prices or totals.")
    public ResponseEntity<OrderResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(OrderResponse.from(queries.get(id)));
    }
}
