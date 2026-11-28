package com.beautyshop.service;

import com.beautyshop.order.OrderStatus;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SnapshotAuditTest {
    @Test void chiChapNhanKieuNghiepVu() {
        assertThat(SuKienQuanTri_Service.snapshot(null)).isNull();
        assertThat(SuKienQuanTri_Service.snapshot(OrderStatus.CONFIRMED)).isEqualTo("CONFIRMED");
        assertThat(SuKienQuanTri_Service.snapshot(8)).isEqualTo("8");
        assertThatThrownBy(() -> SuKienQuanTri_Service.snapshot("raw")).isInstanceOf(IllegalArgumentException.class);
    }
}
