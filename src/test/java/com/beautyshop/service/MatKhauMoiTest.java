package com.beautyshop.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatKhauMoiTest {
    @Test
    void soSanhVoiHashHienTaiVaDatLanDauKhiChuaCoHash() {
        var encoder = new BCryptPasswordEncoder(4);
        String cu = java.util.UUID.randomUUID().toString();
        String moi = java.util.UUID.randomUUID().toString();
        String bam = MatKhau_Service.bamMatKhauMoi(cu, cu, null, encoder);
        assertThat(encoder.matches(cu, bam)).isTrue();
        assertThatThrownBy(() -> MatKhau_Service.bamMatKhauMoi(cu, cu, bam, encoder))
            .isInstanceOf(MatKhau_Service.MatKhauMoiKhongHopLe.class);
        assertThat(encoder.matches(moi, MatKhau_Service.bamMatKhauMoi(moi, moi, bam, encoder))).isTrue();
    }

    @Test
    void kiemXacNhanSoKyTuVaSoByteUtf8() {
        String bien = "😀".repeat(18);
        assertThat(MatKhau_Service.matKhauMoiHopLe(bien, bien)).isTrue();
        assertThat(MatKhau_Service.matKhauMoiHopLe(bien + "a", bien + "a")).isFalse();
        assertThat(MatKhau_Service.matKhauMoiHopLe("😀".repeat(11), "😀".repeat(11))).isFalse();
        assertThat(MatKhau_Service.matKhauMoiHopLe(bien, "khac")).isFalse();
        assertThat(MatKhau_Service.matKhauMoiHopLe(null, null)).isFalse();
    }
}
