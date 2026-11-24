package com.beautyshop.order;

public enum OrderStatus {
    PENDING_PAYMENT("Chờ thanh toán"),
    PENDING_CONFIRMATION("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    PACKING("Đang đóng gói"),
    SHIPPING("Đang giao hàng"),
    DELIVERED("Đã giao"),
    CANCELLED("Đã hủy"),
    EXPIRED("Đã hết hạn");

    private final String label;
    OrderStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
