package com.beautyshop.order;

import java.util.List;

public enum ReturnStatus {
    PENDING("Chờ duyệt"), APPROVED("Đã duyệt"), REJECTED("Đã từ chối"),
    RECEIVED("Đã nhận hàng"), COMPLETED("Đã hoàn tất");

    private final String label;
    ReturnStatus(String label) { this.label = label; }
    public String getLabel() { return label; }

    public List<ReturnStatus> buocTiepTheo() {
        return switch (this) {
            case PENDING -> List.of(APPROVED, REJECTED);
            case APPROVED -> List.of(RECEIVED);
            case RECEIVED -> List.of(COMPLETED);
            default -> List.of();
        };
    }
}
