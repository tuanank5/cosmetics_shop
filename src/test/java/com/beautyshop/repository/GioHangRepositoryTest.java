package com.beautyshop.repository;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GioHangRepositoryTest {
    private static final String ANH = "/uploads/sku/00000000-0000-4000-8000-000000000026.png";
    // Two catalog R026 variants and two carts. Each SELECT supplies its own read-only test rows.
    private static GioHang_Repository repository(boolean sanPhamHoatDong, boolean thuongHieuHoatDong) {
        String duLieu = """
            WITH thuong_hieu(ma_thuong_hieu, ten_thuong_hieu, dang_hoat_dong) AS (VALUES (1, 'Vanicream', %s)),
            danh_muc(ma_danh_muc, dang_hoat_dong) AS (VALUES (1, TRUE)),
            san_pham(ma_san_pham, ma_thuong_hieu, ma_danh_muc, ten_san_pham, duong_dan, dang_hoat_dong)
                AS (VALUES (26, 1, 1, 'Moisturizing Cream', 'demo-r026', %s)),
            bien_the_san_pham(ma_bien_the, ma_san_pham, ten_bien_the, sku, gia_ban, so_luong_trong_kho,
                              so_luong_tam_giu, dang_hoat_dong, url_hinh_anh)
                AS (VALUES (11, 26, '16 oz', 'DEMO-026', 410000, 7, 1, TRUE, '%s'),
                           (22, 26, '4 oz', 'DEMO-026-B', 245000, 1, 1, TRUE, NULL)),
            gio_hang(ma_gio_hang, ma_nguoi_dung) AS (VALUES (1, 7), (2, 8)),
            chi_tiet_gio_hang(ma_gio_hang, ma_bien_the, so_luong) AS (VALUES (1, 11, 2), (1, 22, 1), (2, 11, 4))
            """.formatted(thuongHieuHoatDong, sanPhamHoatDong, ANH);
        var h2 = JdbcClient.create(new DriverManagerDataSource("jdbc:h2:mem:;MODE=MariaDB"));
        var jdbc = mock(JdbcClient.class);
        when(jdbc.sql(anyString())).thenAnswer(call -> h2.sql(duLieu + call.getArgument(0, String.class)));
        return new GioHang_Repository(jdbc);
    }

    @Test
    void haiSkuKhongNhanDongVaKhongLayGioNguoiKhac() {
        var repository = repository(true, true);
        var gio = repository.docGio(7);
        assertThat(gio).hasSize(2);
        var lon = gio.getFirst();
        var nho = gio.getLast();
        assertThat(lon.maBienThe()).isEqualTo(11);
        assertThat(nho.maBienThe()).isEqualTo(22);
        assertThat(lon.urlHinhAnh()).isEqualTo(ANH);
        assertThat(nho.urlHinhAnh()).isNull();
        assertThat(gio).allMatch(m -> "Vanicream".equals(m.tenThuongHieu()) && "demo-r026".equals(m.duongDan()));
        assertThat(lon.khaDung()).isEqualTo(6);
        assertThat(nho.khaDung()).isZero();
        assertThat(lon.soLuong()).isEqualTo(2);
        assertThat(nho.soLuong()).isEqualTo(1);
        assertThat(lon.giaBan().multiply(BigDecimal.valueOf(lon.soLuong())).add(nho.giaBan())).isEqualByComparingTo("1065000");
        assertThat(repository.docGio(8)).singleElement().isEqualTo(lon.voiSoLuong(4));
        assertThat(repository.timBienThe(11, false)).contains(lon.voiSoLuong(0));
        assertThat(repository.timBienThe(22, false)).contains(nho.voiSoLuong(0));
    }

    @ParameterizedTest
    @CsvSource({"false,true", "true,false"})
    void ngungHienThiVanGiuGioVaGia(boolean sanPhamHoatDong, boolean thuongHieuHoatDong) {
        var repository = repository(sanPhamHoatDong, thuongHieuHoatDong);
        var gio = repository.docGio(7);
        assertThat(gio).hasSize(2).allMatch(m -> m.duongDan() == null && m.khaDung() == 0);
        assertThat(gio.getFirst().soLuong()).isEqualTo(2);
        assertThat(gio.getFirst().giaBan()).isEqualByComparingTo("410000");
        assertThat(repository.timBienThe(11, false)).contains(gio.getFirst().voiSoLuong(0));
    }
    @ParameterizedTest
    @ValueSource(strings = {"/uploads/sku/123e4567-e89b-12d3-a456-426614174000.png",
        "/img/products/r001.webp", "/img/products/r026-a.jpg", "/img/products/demo-r010.png"})
    void anhUploadVaAnhDemoTrongJarDuocGiu(String url) {
        assertThat(new GioHang_Repository.MucGio(1, "A", "B", BigDecimal.ONE, 1, 1, "S", url, null, null).urlHinhAnh())
            .isEqualTo(url);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/img/products/../app.js", "/img/products/R001.webp", "/img/products/r001.gif",
        "/img/products/r001.webp?x=1", "/img/products/-r001.webp", "/img/products/a/b.png", "/img/editorial/light.svg",
        "https://example.com/a.png", "//example.com/a.png", "/uploads/sku/fixture.png", "javascript:alert(1)"})
    void anhNgoaiMauBiLoai(String url) {
        assertThat(new GioHang_Repository.MucGio(1, "A", "B", BigDecimal.ONE, 1, 1, "S", url, null, null).urlHinhAnh())
            .isNull();
    }
}
