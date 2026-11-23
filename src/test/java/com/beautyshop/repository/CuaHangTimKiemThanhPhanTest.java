package com.beautyshop.repository;

import com.beautyshop.repository.CuaHang_Repository.DanhMucDangBan;
import com.beautyshop.repository.CuaHang_Repository.TheSanPham;
import com.beautyshop.service.CuaHang_Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** P18 (4.4): /products tìm cả trong thành phần; escape % và _ giữ nguyên. Chạy thật JPQL trên H2. */
@DataJpaTest
@TestPropertySource(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class CuaHangTimKiemThanhPhanTest {
    @Autowired CuaHang_Repository cuaHangRepository;
    @Autowired BienTheSanPham_Repository bienTheRepository;
    @Autowired DanhMuc_Repository danhMucRepository;
    @Autowired ThuongHieu_Repository thuongHieuRepository;
    @Autowired JdbcClient jdbc;
    CuaHang_Service service;

    @BeforeEach
    void duLieu() {
        service = new CuaHang_Service(cuaHangRepository, bienTheRepository, danhMucRepository, thuongHieuRepository);
        jdbc.sql("INSERT INTO thuong_hieu (ma_thuong_hieu, ten_thuong_hieu, duong_dan, dang_hoat_dong) VALUES (1, 'Brand', 'brand', TRUE)").update();
        jdbc.sql("INSERT INTO danh_muc (ma_danh_muc, ten_danh_muc, duong_dan, dang_hoat_dong) VALUES (1, 'Dưỡng ẩm', 'duong-am', TRUE)").update();
        jdbc.sql("INSERT INTO danh_muc (ma_danh_muc, ten_danh_muc, duong_dan, dang_hoat_dong) VALUES (2, 'Làm sạch', 'lam-sach', TRUE)").update();
        jdbc.sql("INSERT INTO danh_muc (ma_danh_muc, ten_danh_muc, duong_dan, dang_hoat_dong) VALUES (3, 'Ẩn', 'an', FALSE)").update();
        sanPham(1, 1, "gel-am", "Water Gel", "Gel cấp ẩm.", "Hyaluronic acid, glycerin", "Da khô, da nhạy cảm", true);
        sanPham(2, 2, "sua-rua-mat", "Cleanser", "Làm sạch dịu nhẹ.", "Salicylic acid 2%", "da dầu", true);
        sanPham(3, 1, "kem-am", "Cream", "Kem dưỡng.", "Ceramide_NP, cholesterol", "Da khô", true);
        sanPham(4, 1, "an", "Hidden hyaluronic", "Ẩn.", "Hyaluronic acid", "Da hỗn hợp", false);
        sanPham(5, 3, "danh-muc-an", "Other", "Khác.", "Hyaluronic acid", "Da thường", true);
        for (long ma = 1; ma <= 5; ma++) {
            jdbc.sql("""
                INSERT INTO bien_the_san_pham (ma_bien_the, ma_san_pham, sku, ten_bien_the, gia_ban,
                    so_luong_trong_kho, so_luong_tam_giu, dang_hoat_dong) VALUES (?, ?, ?, '30 ml', 100000, 5, 0, TRUE)
                """).params(ma, ma, "SKU-" + ma).update();
        }
    }

    private void sanPham(long ma, long danhMuc, String slug, String ten, String moTa, String thanhPhan, String loaiDa, boolean active) {
        jdbc.sql("""
            INSERT INTO san_pham (ma_san_pham, ma_thuong_hieu, ma_danh_muc, ten_san_pham, duong_dan, mo_ta, thanh_phan,
                loai_da_phu_hop, dang_hoat_dong, ngay_tao) VALUES (?, 1, ?, ?, ?, ?, ?, ?, ?, TIMESTAMP '2026-01-01 00:00:00')
            """).params(ma, danhMuc, ten, slug, moTa, thanhPhan, loaiDa, active).update();
    }

    private java.util.List<String> tim(String q) {
        return service.timSanPham(q, "", "", "", "newest", PageRequest.of(0, 20)).getContent().stream()
            .map(TheSanPham::getDuongDan).toList();
    }

    @Test
    void timTrongThanhPhanChiLaySanPhamDangBan() {
        assertThat(tim("hyaluronic")).containsExactly("gel-am");
        assertThat(tim("HYALURONIC")).containsExactly("gel-am");
        assertThat(tim("ceramide")).containsExactly("kem-am");
        assertThat(tim("salicylic")).containsExactly("sua-rua-mat");
        assertThat(tim("niacinamide")).isEmpty();
    }

    @Test
    void phanTramVaGachDuoiVanDuocEscape() {
        assertThat(tim("%")).containsExactly("sua-rua-mat");
        assertThat(tim("2%")).containsExactly("sua-rua-mat");
        assertThat(tim("_")).containsExactly("kem-am");
        assertThat(tim("ceramide_np")).containsExactly("kem-am");
        assertThat(tim("ceramide%np")).isEmpty();
    }

    @Test
    void timKiemCuVanHoatDong() {
        assertThat(tim("water")).containsExactly("gel-am");
        assertThat(tim("dịu nhẹ")).containsExactly("sua-rua-mat");
        assertThat(tim("sku-3")).containsExactly("kem-am");
        assertThat(service.timSanPham("", "", "", "Da khô", "newest", PageRequest.of(0, 20)).getTotalElements()).isEqualTo(2);
    }

    @Test
    void truyVanTrangChuChiDemSanPhamDangBan() {
        assertThat(cuaHangRepository.timLoaiDaDangBan()).containsExactlyInAnyOrder("Da khô, da nhạy cảm", "da dầu", "Da khô");
        assertThat(cuaHangRepository.demDanhMucDangBan())
            .extracting(DanhMucDangBan::getDuongDan, DanhMucDangBan::getSoSanPham)
            .containsExactlyInAnyOrder(org.assertj.core.groups.Tuple.tuple("duong-am", 2L),
                org.assertj.core.groups.Tuple.tuple("lam-sach", 1L));
    }
}
