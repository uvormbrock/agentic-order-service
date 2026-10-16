package com.example.agenticorderservice.order.persistence;

import com.example.agenticorderservice.order.domain.Order;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends Repository<Order, UUID> {
    Order save(Order entity);
    void flush();
    Optional<Order> findById(UUID id);
    @Transactional(readOnly = true)
    @Query("select distinct o from PurchaseOrder o left join fetch o.items where o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") UUID id);
    boolean existsByCustomerId(UUID customerId);
    @Query("select distinct o from PurchaseOrder o left join fetch o.items")
    java.util.List<Order> findAllWithItems();
}
