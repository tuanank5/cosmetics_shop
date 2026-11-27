package com.beautyshop.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LienHeCuaHangTest {
    @Test
    void hienThiCacKenhDuocCauHinhHopLe() {
        var lienHe = new LienHeCuaHang(" +84 000 000 000 ", " contact@example.invalid ",
            "https://zalo.me/fixture", "https://www.facebook.com/fixture", "https://instagram.com/fixture/");
        assertThat(lienHe.goiDien()).isEqualTo("tel:+84000000000");
        assertThat(lienHe.guiEmail()).isEqualTo("mailto:contact@example.invalid");
        assertThat(lienHe.zalo()).isEqualTo("https://zalo.me/fixture");
        assertThat(lienHe.facebook()).isEqualTo("https://www.facebook.com/fixture");
        assertThat(lienHe.instagram()).isEqualTo("https://instagram.com/fixture/");
    }

    @Test
    void anKenhTrongSaiGiaoThucVaTrangMangXaHoiChung() {
        var lienHe = new LienHeCuaHang(" ", "example@invalid?subject=hidden", "http://zalo.me/fixture",
            "https://facebook.com.evil.invalid/fixture", "https://www.instagram.com/?ignored=true");
        assertThat(lienHe.goiDien()).isNull();
        assertThat(lienHe.guiEmail()).isNull();
        assertThat(lienHe.zalo()).isNull();
        assertThat(lienHe.facebook()).isNull();
        assertThat(lienHe.instagram()).isNull();
    }
}
