package com.beautyshop.order;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderStatePolicyTest {
    @Test void allowNormalProgress() {
        assertTrue(OrderStatePolicy.canMove(OrderStatus.CONFIRMED, OrderStatus.PACKING));
        assertTrue(OrderStatePolicy.canMove(OrderStatus.SHIPPING, OrderStatus.DELIVERED));
    }
    @Test void blockInvalidProgress() {
        assertFalse(OrderStatePolicy.canMove(OrderStatus.PENDING_PAYMENT, OrderStatus.DELIVERED));
        assertFalse(OrderStatePolicy.canMove(OrderStatus.CANCELLED, OrderStatus.CONFIRMED));
        assertFalse(OrderStatePolicy.canMove(OrderStatus.DELIVERED, OrderStatus.CANCELLED));
    }
}
