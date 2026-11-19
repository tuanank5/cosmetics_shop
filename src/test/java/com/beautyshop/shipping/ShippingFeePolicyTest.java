package com.beautyshop.shipping;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ShippingFeePolicyTest {
    @Test void feeBelowThreshold() {
        assertEquals(new BigDecimal("25000"), ShippingFeePolicy.calculate(new BigDecimal("499999")));
    }
    @Test void freeAtThreshold() {
        assertEquals(BigDecimal.ZERO, ShippingFeePolicy.calculate(new BigDecimal("500000")));
    }
    @Test void rejectNegativeAmount() {
        assertThrows(IllegalArgumentException.class,
            () -> ShippingFeePolicy.calculate(new BigDecimal("-1")));
    }
}
