package com.beautyshop.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TuVanDaRepositoryTest {
    @Autowired BienTheSanPham_Repository repository;
    @Autowired JdbcClient jdbc;

    @BeforeEach
    void taoCatalog() {
        jdbc.sql("""
                INSERT INTO thuong_hieu
                    (ma_thuong_hieu, ten_thuong_hieu, duong_dan, dang_hoat_dong)
                VALUES (1, 'Brand', 'brand', TRUE)
                """).update();
        danhMuc(1, "Tinh chất");
        danhMuc(2, "Sữa rửa mặt");
        danhMuc(3, "Làm sạch");
        danhMuc(4, "Toner & xịt khoáng");

        sanPham(1, 1, "Serum kiểm soát dầu", true);
        sanPham(2, 1, "Vitamin C 12%", true);
        sanPham(3, 2, "Sữa rửa mặt dịu nhẹ", true);
        sanPham(4, 3, "Foaming Face Wash", true);
        sanPham(5, 3, "Cleansing Oil", true);
        sanPham(6, 4, "Toner cấp ẩm", true);
        sanPham(7, 1, "Serum hết hàng", true);

        bienThe(1, 1, 5);
        bienThe(2, 2, 5);
        bienThe(3, 3, 5);
        bienThe(4, 4, 5);
        bienThe(5, 5, 5);
        bienThe(6, 6, 5);
        bienThe(7, 7, 0);
    }

    @Test
    void locLoaiSanPhamTrenTenVaDanhMucVaLoaiBoHetHang() {
        assertThat(maBienThe("serum")).containsExactly(1L);
        assertThat(maBienThe("tinh chất")).containsExactly(1L, 2L);
        assertThat(maBienThe("sữa rửa mặt")).containsExactly(3L);
        assertThat(maBienThe("face wash")).containsExactly(4L);
        assertThat(maBienThe("toner")).containsExactly(6L);
    }

    private java.util.List<Long> maBienThe(String tuKhoa) {
        return repository.findDangBanConHangTheoLoai(tuKhoa, PageRequest.of(0, 20))
                .stream()
                .map(item -> item.getMaBienThe())
                .toList();
    }

    private void danhMuc(long ma, String ten) {
        jdbc.sql("""
                INSERT INTO danh_muc
                    (ma_danh_muc, ten_danh_muc, duong_dan, dang_hoat_dong)
                VALUES (?, ?, ?, TRUE)
                """).params(ma, ten, "dm-" + ma).update();
    }

    private void sanPham(long ma, long maDanhMuc, String ten, boolean dangHoatDong) {
        jdbc.sql("""
                INSERT INTO san_pham (
                    ma_san_pham, ma_thuong_hieu, ma_danh_muc, ten_san_pham, duong_dan,
                    dang_hoat_dong, ngay_tao)
                VALUES (?, 1, ?, ?, ?, ?, TIMESTAMP '2026-01-01 00:00:00')
                """).params(ma, maDanhMuc, ten, "sp-" + ma, dangHoatDong).update();
    }

    private void bienThe(long ma, long maSanPham, int ton) {
        jdbc.sql("""
                INSERT INTO bien_the_san_pham (
                    ma_bien_the, ma_san_pham, sku, ten_bien_the, gia_ban,
                    so_luong_trong_kho, so_luong_tam_giu, dang_hoat_dong)
                VALUES (?, ?, ?, '30 ml', 100000, ?, 0, TRUE)
                """).params(ma, maSanPham, "SKU-" + ma, ton).update();
    }
}
