package com.farmconnect.service.impl;

import com.farmconnect.dto.request.PlaceOrderRequest;
import com.farmconnect.dto.response.OrderResponse;
import com.farmconnect.entity.*;
import com.farmconnect.exception.BadRequestException;
import com.farmconnect.exception.ResourceNotFoundException;
import com.farmconnect.repository.*;
import com.farmconnect.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final NotificationServiceImpl notificationService;
    private final FarmerServiceImpl farmerService;

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest req) {
        Long customerId = SecurityUtil.currentUserId();
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Cart cart = cartRepository.findByCustomer_UserId(customerId)
                .orElseThrow(() -> new BadRequestException("Your cart is empty"));

        if (cart.getCartItems().isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        // Validate stock for every item before committing anything.
        for (CartItem ci : cart.getCartItems()) {
            Product product = ci.getProduct();
            if (!product.getAvailability() || ci.getQuantity() > product.getQuantity()) {
                throw new BadRequestException("'" + product.getProductName() + "' no longer has enough stock");
            }
        }

        BigDecimal total = cart.getCartItems().stream()
                .map(ci -> ci.getProduct().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .customer(customer)
                .totalAmount(total)
                .shippingAddress(req.getShippingAddress())
                .contactPhone(req.getContactPhone())
                .paymentMethod(req.getPaymentMethod())
                .paymentStatus(req.getPaymentMethod() == Order.PaymentMethod.COD
                        ? Order.PaymentStatus.COD : Order.PaymentStatus.PENDING)
                .orderStatus(Order.OrderStatus.PENDING)
                .build();
        order = orderRepository.save(order);

        for (CartItem ci : cart.getCartItems()) {
            Product product = ci.getProduct();

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .farmer(product.getFarmer())
                    .quantity(ci.getQuantity())
                    .price(product.getPrice())
                    .subtotal(product.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                    .build();
            orderItemRepository.save(orderItem);
            order.getOrderItems().add(orderItem);

            // decrement stock, and force unavailable the moment stock hits
            // zero (a manually-set availability=true flag must never
            // survive 0 stock)
            product.setQuantity(product.getQuantity() - ci.getQuantity());
            if (product.getQuantity() <= 0) {
                product.setAvailability(false);
            }
            productRepository.save(product);

            notificationService.notify(product.getFarmer().getUser(),
                    "New order received",
                    "You have a new order for " + ci.getQuantity() + " " + product.getUnit() + " of " + product.getProductName(),
                    Notification.NotificationType.ORDER_UPDATE);
        }

        cart.getCartItems().clear();
        cartRepository.save(cart);

        notificationService.notify(customer, "Order placed",
                "Your order #" + order.getOrderId() + " has been placed successfully.",
                Notification.NotificationType.ORDER_UPDATE);

        return toResponse(order);
    }

@Transactional(readOnly = true)
public List<OrderResponse> myOrders() {
    return orderRepository.findByCustomer_UserIdOrderByCreatedAtDesc(SecurityUtil.currentUserId())
            .stream()
            .map(this::toResponse)
            .toList();
}


    @Transactional(readOnly = true)
public OrderResponse getOrder(Long orderId) {
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    if (!order.getCustomer().getUserId().equals(SecurityUtil.currentUserId())) {
        throw new ResourceNotFoundException("Order not found");
    }
    return toResponse(order);
}

    /**
     * Only PENDING or ACCEPTED orders can be cancelled by the customer -
     * once a farmer has PACKED it, fulfillment is already underway.
     * Ownership is verified against the authenticated customer, never a
     * client-supplied id, so customer A cannot cancel customer B's order
     * by guessing an order id.
     */
    private static final java.util.Set<Order.OrderStatus> CUSTOMER_CANCELLABLE_STATUSES =
            java.util.EnumSet.of(Order.OrderStatus.PENDING, Order.OrderStatus.ACCEPTED);

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getCustomer().getUserId().equals(SecurityUtil.currentUserId())) {
            throw new ResourceNotFoundException("Order not found");
        }

        if (!CUSTOMER_CANCELLABLE_STATUSES.contains(order.getOrderStatus())) {
            throw new BadRequestException(
                    "This order can no longer be cancelled (current status: " + order.getOrderStatus() + ")");
        }

        order.setOrderStatus(Order.OrderStatus.CANCELLED);
        restoreStockForOrder(order);
        orderRepository.save(order);

        notificationService.notify(order.getCustomer(), "Order cancelled",
                "Your order #" + order.getOrderId() + " has been cancelled.",
                Notification.NotificationType.ORDER_UPDATE);

        return toResponse(order);
    }

    /**
     * Returns every item's quantity to its product's stock and re-enables
     * availability if stock is now positive again. Called exactly once per
     * order, only from the CANCELLED/REJECTED transition paths - both are
     * themselves guarded (cancelOrder only fires from PENDING/ACCEPTED,
     * updateStatus's terminal-state guard below rejects any further status
     * change once an order is CANCELLED or REJECTED) so an order can never
     * have its stock restored twice.
     */
    private void restoreStockForOrder(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            int currentQty = product.getQuantity() == null ? 0 : product.getQuantity();
            int itemQty = item.getQuantity() == null ? 0 : item.getQuantity();
            int restoredQty = currentQty + itemQty;
            product.setQuantity(restoredQty);
            if (restoredQty > 0) {
                product.setAvailability(true);
            }
            productRepository.save(product);
        }
    }

    @Transactional(readOnly = true)
public List<OrderResponse> farmerOrders(Long farmerId) {
        return orderItemRepository.findByFarmer_FarmerIdOrderByOrderItemIdDesc(farmerId).stream()
                .map(OrderItem::getOrder).distinct()
                .map(this::toResponse).toList();
    }

    private static final java.util.Map<Order.OrderStatus, java.util.Set<Order.OrderStatus>> ALLOWED_FARMER_TRANSITIONS =
            java.util.Map.of(
                    Order.OrderStatus.PENDING, java.util.EnumSet.of(
                            Order.OrderStatus.ACCEPTED,
                            Order.OrderStatus.REJECTED,
                            Order.OrderStatus.PACKED,
                            Order.OrderStatus.OUT_FOR_DELIVERY,
                            Order.OrderStatus.DELIVERED
                    ),
                    Order.OrderStatus.ACCEPTED, java.util.EnumSet.of(
                            Order.OrderStatus.REJECTED,
                            Order.OrderStatus.PACKED,
                            Order.OrderStatus.OUT_FOR_DELIVERY,
                            Order.OrderStatus.DELIVERED
                    ),
                    Order.OrderStatus.PACKED, java.util.EnumSet.of(
                            Order.OrderStatus.REJECTED,
                            Order.OrderStatus.OUT_FOR_DELIVERY,
                            Order.OrderStatus.DELIVERED
                    ),
                    Order.OrderStatus.OUT_FOR_DELIVERY, java.util.EnumSet.of(
                            Order.OrderStatus.DELIVERED
                    )
            );

    public static boolean isValidFarmerTransition(Order.OrderStatus currentStatus, Order.OrderStatus newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }
        java.util.Set<Order.OrderStatus> allowed = ALLOWED_FARMER_TRANSITIONS.get(currentStatus);
        return allowed != null && allowed.contains(newStatus);
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Long callerFarmerId = farmerService.myProfile().getFarmerId();
        boolean ownsAnItem = order.getOrderItems().stream()
                .anyMatch(oi -> oi.getFarmer().getFarmerId().equals(callerFarmerId));
        if (!ownsAnItem) {
            throw new BadRequestException("This order does not contain any of your products");
        }

        if (status == null) {
            throw new BadRequestException("Status is required");
        }

        if (status == Order.OrderStatus.CANCELLED) {
            throw new BadRequestException("Farmers cannot cancel orders");
        }

        Order.OrderStatus currentStatus = order.getOrderStatus();

        if (currentStatus == Order.OrderStatus.REJECTED) {
            throw new BadRequestException("After the order is Rejected. You could not update the status");
        }
        if (currentStatus == Order.OrderStatus.DELIVERED) {
            throw new BadRequestException("After the Order is Delivered. You could not update the status");
        }
        if (currentStatus == Order.OrderStatus.CANCELLED) {
            throw new BadRequestException("This order is already CANCELLED and its status can no longer be changed");
        }

        java.util.Set<Order.OrderStatus> allowed = ALLOWED_FARMER_TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(status)) {
            throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + status);
        }

        order.setOrderStatus(status);
        if (status == Order.OrderStatus.REJECTED) {
            restoreStockForOrder(order);
        }
        if (status == Order.OrderStatus.DELIVERED && order.getPaymentMethod() == Order.PaymentMethod.COD) {
            order.setPaymentStatus(Order.PaymentStatus.PAID);
        }
        orderRepository.save(order);

        notificationService.notify(order.getCustomer(), "Order update",
                "Your order #" + order.getOrderId() + " is now " + status,
                Notification.NotificationType.ORDER_UPDATE);

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> allOrders() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items = order.getOrderItems().stream().map(oi ->
                OrderResponse.OrderItemResponse.builder()
                        .orderItemId(oi.getOrderItemId())
                        .productId(oi.getProduct().getProductId())
                        .productName(oi.getProduct().getProductName())
                        .imageUrl(oi.getProduct().getImageUrl())
                        .farmerId(oi.getFarmer().getFarmerId())
                        .farmName(oi.getFarmer().getFarmName())
                        .quantity(oi.getQuantity())
                        .price(oi.getPrice())
                        .subtotal(oi.getSubtotal())
                        .build()
        ).toList();

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .customerId(order.getCustomer().getUserId())
                .customerName(order.getCustomer().getName())
                .customerPhone(order.getContactPhone())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .orderStatus(order.getOrderStatus())
                .items(items)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
