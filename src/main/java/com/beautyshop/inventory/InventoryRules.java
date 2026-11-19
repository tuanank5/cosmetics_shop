package com.beautyshop.inventory;

/** Công thức tồn kho, KHÔNG thực hiện trừ kho hoặc khóa DB. */
public final class InventoryRules {
    private InventoryRules() {}

    public static int available(int soLuongTrongKho, int soLuongTamGiu) {
        if (soLuongTrongKho < 0 || soLuongTamGiu < 0 || soLuongTamGiu > soLuongTrongKho) {
            throw new IllegalArgumentException("Số lượng tồn kho không hợp lệ");
        }
        return soLuongTrongKho - soLuongTamGiu;
    }
}
