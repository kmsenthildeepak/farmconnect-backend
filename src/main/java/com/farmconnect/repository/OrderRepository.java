package com.farmconnect.repository;

import com.farmconnect.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomer_UserIdOrderByCreatedAtDesc(Long customerId);
    List<Order> findByOrderStatus(Order.OrderStatus status);
}
