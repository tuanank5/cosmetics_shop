package com.beautyshop.shipping;

import java.math.BigDecimal;

/** Chính sách demo: 25.000 VNĐ; miễn phí từ 500.000 VNĐ (tạm tính sau giảm giá). */
public final class ShippingFee_Policy {
    public static final BigDecimal STANDARD_FEE = new BigDecimal("25000");
    public static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500000");

    private ShippingFee_Policy() {}

    public static BigDecimal calculate(BigDecimal tamTinhSauGiamGia) {
        if (tamTinhSauGiamGia == null || tamTinhSauGiamGia.signum() < 0) {
            throw new IllegalArgumentException("Giá trị đơn không hợp lệ");
        }
        return tamTinhSauGiamGia.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
            ? BigDecimal.ZERO : STANDARD_FEE;
    }
}
