package com.beautyshop.inventory;

/** Công thức tồn kho, KHÔNG thực hiện trừ kho hoặc khóa DB. */
public final class InventoryRules {
    private InventoryRules() {}

    public static int available(int onHand, int reserved) {
        if (onHand < 0 || reserved < 0 || reserved > onHand) {
            throw new IllegalArgumentException("Số lượng tồn kho không hợp lệ");
        }
        return onHand - reserved;
    }
}
