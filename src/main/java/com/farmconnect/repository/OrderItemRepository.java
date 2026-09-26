package com.farmconnect.repository;

import com.farmconnect.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByFarmer_FarmerIdOrderByOrderItemIdDesc(Long farmerId);
    List<OrderItem> findByOrder_OrderId(Long orderId);

    boolean existsByOrder_Customer_UserIdAndProduct_ProductIdAndOrder_OrderStatus(
            Long customerId, Long productId, com.farmconnect.entity.Order.OrderStatus status);
}
