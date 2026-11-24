package com.beautyshop.order;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderStatePolicyTest {
    @Test void allowNormalProgress() {
        assertTrue(OrderState_Policy.canMove(OrderStatus.CONFIRMED, OrderStatus.PACKING));
        assertTrue(OrderState_Policy.canMove(OrderStatus.SHIPPING, OrderStatus.DELIVERED));
    }
    @Test void blockInvalidProgress() {
        assertFalse(OrderState_Policy.canMove(OrderStatus.PENDING_PAYMENT, OrderStatus.DELIVERED));
        assertFalse(OrderState_Policy.canMove(OrderStatus.CANCELLED, OrderStatus.CONFIRMED));
        assertFalse(OrderState_Policy.canMove(OrderStatus.DELIVERED, OrderStatus.CANCELLED));
    }
}
