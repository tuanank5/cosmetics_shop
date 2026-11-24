package com.beautyshop.repository;

import com.beautyshop.repository.CuaHang_Repository.TheSanPham;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** Chạy thật truy vấn JPQL của dải nổi bật trên H2; MariaDB vẫn được phủ bởi CuaHang_MariaDb_Test khi bật cờ. */
@DataJpaTest
@TestPropertySource(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class CuaHangNoiBatRepositoryTest {
    @Autowired CuaHang_Repository cuaHangRepository;
    @Autowired JdbcClient jdbc;

    @Test
    void chiLaySanPhamActiveConHangMoiNhatVaGioiHanSoLuong() {
        thuongHieu(1, true);
        thuongHieu(2, false);
        danhMuc(1, true);
        danhMuc(2, false);
        sanPham(1, 1, 1, "cu-nhat", true, 1);
        sanPham(2, 1, 1, "moi-hon", true, 2);
        sanPham(3, 1, 1, "moi-nhat", true, 3);
        sanPham(4, 1, 1, "het-hang", true, 9);
        sanPham(5, 1, 1, "an", false, 9);
        sanPham(6, 2, 1, "thuong-hieu-an", true, 9);
        sanPham(7, 1, 2, "danh-muc-an", true, 9);
        sanPham(8, 1, 1, "sku-con-hang-bi-an", true, 9);
        sanPham(9, 1, 1, "chua-co-sku", true, 9);
        bienThe(1, 1, 200000, 5, 0, true, null);
        bienThe(2, 2, 300000, 2, 0, true, "/uploads/sku/a.png");
        bienThe(3, 2, 150000, 1, 1, true, null);
        bienThe(4, 3, 90000, 1, 0, true, null);
        bienThe(5, 4, 10000, 3, 3, true, null);
        bienThe(6, 5, 10000, 3, 0, true, null);
        bienThe(7, 6, 10000, 3, 0, true, null);
        bienThe(8, 7, 10000, 3, 0, true, null);
        bienThe(9, 8, 10000, 0, 0, true, null);
        bienThe(10, 8, 10000, 9, 0, false, null);

        var tatCa = cuaHangRepository.timNoiBat(PageRequest.of(0, 8));
        assertThat(tatCa).extracting(TheSanPham::getDuongDan).containsExactly("moi-nhat", "moi-hon", "cu-nhat");
        var moiHon = tatCa.get(1);
        assertThat(moiHon.getGiaTu()).isEqualByComparingTo("150000");
        assertThat(moiHon.getSoLuongKhaDungLonNhat()).isEqualTo(2);
        assertThat(moiHon.getUrlHinhAnh()).isEqualTo("/uploads/sku/a.png");
        assertThat(moiHon.getTenThuongHieu()).isEqualTo("Thương hiệu 1");

        assertThat(cuaHangRepository.timNoiBat(PageRequest.of(0, 2))).extracting(TheSanPham::getDuongDan)
            .containsExactly("moi-nhat", "moi-hon");
    }

    private void thuongHieu(long ma, boolean active) {
        jdbc.sql("INSERT INTO thuong_hieu (ma_thuong_hieu, ten_thuong_hieu, duong_dan, dang_hoat_dong) VALUES (?, ?, ?, ?)")
            .params(ma, "Thương hiệu " + ma, "th-" + ma, active).update();
    }

    private void danhMuc(long ma, boolean active) {
        jdbc.sql("INSERT INTO danh_muc (ma_danh_muc, ten_danh_muc, duong_dan, dang_hoat_dong) VALUES (?, ?, ?, ?)")
            .params(ma, "Danh mục " + ma, "dm-" + ma, active).update();
    }

    private void sanPham(long ma, long thuongHieu, long danhMuc, String slug, boolean active, int ngay) {
        jdbc.sql("""
            INSERT INTO san_pham (ma_san_pham, ma_thuong_hieu, ma_danh_muc, ten_san_pham, duong_dan, dang_hoat_dong, ngay_tao)
            VALUES (?, ?, ?, ?, ?, ?, DATEADD(DAY, ?, TIMESTAMP '2026-01-01 00:00:00'))
            """).params(ma, thuongHieu, danhMuc, "Sản phẩm " + slug, slug, active, ngay).update();
    }

    private void bienThe(long ma, long sanPham, int gia, int ton, int giu, boolean active, String url) {
        jdbc.sql("""
            INSERT INTO bien_the_san_pham (ma_bien_the, ma_san_pham, sku, ten_bien_the, gia_ban,
                so_luong_trong_kho, so_luong_tam_giu, dang_hoat_dong, url_hinh_anh)
            VALUES (?, ?, ?, '30 ml', ?, ?, ?, ?, ?)
            """).params(ma, sanPham, "SKU-" + ma, gia, ton, giu, active, url).update();
    }
}
