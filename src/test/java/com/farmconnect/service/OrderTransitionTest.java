package com.farmconnect.service;

import com.farmconnect.entity.Order;
import com.farmconnect.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OrderTransitionTest {

    @Test
    public void testPendingTransitions() {
        assertTrue(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.PENDING, Order.OrderStatus.ACCEPTED));
        assertTrue(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.PENDING, Order.OrderStatus.REJECTED));
        assertTrue(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.PENDING, Order.OrderStatus.PACKED));
        assertTrue(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.PENDING, Order.OrderStatus.OUT_FOR_DELIVERY));
        assertTrue(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.PENDING, Order.OrderStatus.DELIVERED));

        assertFalse(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.PENDING, Order.OrderStatus.PENDING));
        assertFalse(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.PENDING, Order.OrderStatus.CANCELLED));
    }

    @Test
    public void testAcceptedTransitions() {
        assertTrue(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.ACCEPTED, Order.OrderStatus.REJECTED));
        assertTrue(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.ACCEPTED, Order.OrderStatus.PACKED));
        assertTrue(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.ACCEPTED, Order.OrderStatus.OUT_FOR_DELIVERY));
        assertTrue(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.ACCEPTED, Order.OrderStatus.DELIVERED));

        assertFalse(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.ACCEPTED, Order.OrderStatus.PENDING));
        assertFalse(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.ACCEPTED, Order.OrderStatus.ACCEPTED));
        assertFalse(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.ACCEPTED, Order.OrderStatus.CANCELLED));
    }

    @Test
    public void testPackedTransitions() {
        assertTrue(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.PACKED, Order.OrderStatus.REJECTED));
        assertTrue(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.PACKED, Order.OrderStatus.OUT_FOR_DELIVERY));
        assertTrue(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.PACKED, Order.OrderStatus.DELIVERED));

        assertFalse(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.PACKED, Order.OrderStatus.PENDING));
        assertFalse(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.PACKED, Order.OrderStatus.ACCEPTED));
        assertFalse(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.PACKED, Order.OrderStatus.PACKED));
        assertFalse(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.PACKED, Order.OrderStatus.CANCELLED));
    }

    @Test
    public void testOutForDeliveryTransitions() {
        assertTrue(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.OUT_FOR_DELIVERY, Order.OrderStatus.DELIVERED));

        assertFalse(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.OUT_FOR_DELIVERY, Order.OrderStatus.PENDING));
        assertFalse(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.OUT_FOR_DELIVERY, Order.OrderStatus.ACCEPTED));
        assertFalse(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.OUT_FOR_DELIVERY, Order.OrderStatus.REJECTED));
        assertFalse(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.OUT_FOR_DELIVERY, Order.OrderStatus.PACKED));
        assertFalse(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.OUT_FOR_DELIVERY, Order.OrderStatus.OUT_FOR_DELIVERY));
        assertFalse(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.OUT_FOR_DELIVERY, Order.OrderStatus.CANCELLED));
    }

    @Test
    public void testRejectedTransitions() {
        for (Order.OrderStatus target : Order.OrderStatus.values()) {
            assertFalse(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.REJECTED, target));
        }
    }

    @Test
    public void testDeliveredTransitions() {
        for (Order.OrderStatus target : Order.OrderStatus.values()) {
            assertFalse(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.DELIVERED, target));
        }
    }

    @Test
    public void testCancelledTransitions() {
        for (Order.OrderStatus target : Order.OrderStatus.values()) {
            assertFalse(OrderServiceImpl.isValidFarmerTransition(Order.OrderStatus.CANCELLED, target));
        }
    }
}
