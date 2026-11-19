package com.beautyshop.order;

import java.util.EnumSet;
import java.util.Map;

/** Luồng order ở mức domain; các điều kiện thanh toán/giữ hàng cần kiểm tra thêm tại service. */
public final class OrderStatePolicy {
    private static final Map<OrderStatus, EnumSet<OrderStatus>> TRANSITIONS = Map.of(
        OrderStatus.PENDING_PAYMENT, EnumSet.of(OrderStatus.PENDING_CONFIRMATION, OrderStatus.CANCELLED, OrderStatus.EXPIRED),
        OrderStatus.PENDING_CONFIRMATION, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
        OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PACKING, OrderStatus.CANCELLED),
        OrderStatus.PACKING, EnumSet.of(OrderStatus.SHIPPING),
        OrderStatus.SHIPPING, EnumSet.of(OrderStatus.DELIVERED),
        OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class),
        OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class),
        OrderStatus.EXPIRED, EnumSet.noneOf(OrderStatus.class)
    );

    private OrderStatePolicy() {}

    public static boolean canMove(OrderStatus current, OrderStatus next) {
        return current != null && next != null && TRANSITIONS.get(current).contains(next);
    }
}
