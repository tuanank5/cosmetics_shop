package com.beautyshop.dto;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LanThanhToanTest {
    @Test
    void hienThiDuLieuLuuVaThoiGianVietNam() {
        for (var muc : BoLocDonHangAdmin.ThanhToan.values()) {
            var lan = new LanThanhToan_Dto(null, "MOMO_MOCK", null, muc.name(), null, null, null, null);
            assertThat(lan.nhanTrangThai()).isEqualTo(muc.nhan);
            assertThat(lan.phuongThuc()).isEqualTo("MoMo mô phỏng");
        }
        var cu = new LanThanhToan_Dto(null, null, null, "UNKNOWN", null, null, null, null);
        assertThat(cu.nhanTrangThai()).isEqualTo("Chưa ghi nhận");
        assertThat(cu.phuongThuc()).isEqualTo("Chưa ghi nhận");
        assertThat(LanThanhToan_Dto.gioVietNam(null)).isEqualTo("Chưa ghi nhận");
        assertThat(LanThanhToan_Dto.gioVietNam(LocalDateTime.parse("2026-11-27T20:01:02")))
            .isEqualTo("28/11/2026 03:01:02");
    }
}
