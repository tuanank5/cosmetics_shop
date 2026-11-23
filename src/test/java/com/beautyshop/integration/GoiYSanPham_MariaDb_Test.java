package com.beautyshop.integration;

import com.beautyshop.recommendation.DiemPhoBien_Policy.UngVien;
import com.beautyshop.repository.CuaHang_Repository;
import com.beautyshop.repository.GoiYSanPham_Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

/** P21: truy vấn ứng viên gợi ý phổ biến chạy thật trên MariaDB. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
class GoiYSanPham_MariaDb_Test {
    @Autowired JdbcClient jdbc;
    @Autowired GoiYSanPham_Repository goiYRepository;
    @Autowired CuaHang_Repository cuaHangRepository;

    @Test
    void demBanDanhGiaYeuThichDungDieuKien() {
        String s = UUID.randomUUID().toString().substring(0, 8);
        long khach = id("INSERT INTO nguoi_dung (email, mat_khau_bam, ho_ten) VALUES (?, 'x', 'Khách gợi ý') RETURNING ma_nguoi_dung", "gy-" + s + "@example.test");
        long th = id("INSERT INTO thuong_hieu (ten_thuong_hieu, duong_dan) VALUES ('TH gợi ý', ?) RETURNING ma_thuong_hieu", "th-gy-" + s);
        long thTat = id("INSERT INTO thuong_hieu (ten_thuong_hieu, duong_dan, dang_hoat_dong) VALUES ('TH tắt', ?, FALSE) RETURNING ma_thuong_hieu", "th-gy-off-" + s);
        long dm = id("INSERT INTO danh_muc (ten_danh_muc, duong_dan) VALUES ('DM gợi ý', ?) RETURNING ma_danh_muc", "dm-gy-" + s);
        long spA = sanPham(th, dm, "a-" + s);
        long spHet = sanPham(th, dm, "het-" + s);
        long spTat = sanPham(thTat, dm, "tat-" + s);
        long btA = bienThe(spA, "GY-A-" + s, 10, 0);
        bienThe(spHet, "GY-H-" + s, 2, 2);   // khả dụng 0 -> không là ứng viên
        bienThe(spTat, "GY-T-" + s, 10, 0);  // thương hiệu tắt -> không là ứng viên

        long donGiao = don(khach, "GY1-" + s, "DELIVERED", 1);
        long ctGiao = dong(donGiao, btA, 3);
        dong(don(khach, "GY2-" + s, "CANCELLED", 1), btA, 5);
        dong(don(khach, "GY3-" + s, "EXPIRED", 1), btA, 7);
        dong(don(khach, "GY4-" + s, "PENDING_PAYMENT", 1), btA, 11);
        dong(don(khach, "GY5-" + s, "DELIVERED", 200), btA, 13);  // ngoài 90 ngày
        long ctAn = dong(don(khach, "GY6-" + s, "DELIVERED", 2), btA, 1);
        long ctCu = dong(don(khach, "GY7-" + s, "DELIVERED", 200), btA, 1);
        danhGia(khach, spA, ctGiao, 4, true, 1);
        danhGia(khach, spA, ctAn, 1, false, 1);   // chờ duyệt -> không tính
        danhGia(khach, spA, ctCu, 1, true, 150);  // ngoài 90 ngày -> không tính
        jdbc.sql("INSERT INTO san_pham_yeu_thich (ma_nguoi_dung, ma_san_pham) VALUES (?, ?)").params(khach, spA).update();

        LocalDateTime tu = jdbc.sql("SELECT UTC_TIMESTAMP(6) - INTERVAL 90 DAY").query(LocalDateTime.class).single();
        Map<Long, UngVien> ungVien = goiYRepository.ungVien(tu).stream()
            .collect(Collectors.toMap(UngVien::maSanPham, Function.identity()));

        assertThat(ungVien).containsKey(spA).doesNotContainKeys(spHet, spTat);
        UngVien a = ungVien.get(spA);
        assertThat(a.soLuongDaBan()).isEqualTo(4);   // 3 + 1, bỏ hủy/hết hạn/chờ thanh toán/ngoài kỳ
        assertThat(a.soDanhGia()).isEqualTo(1);
        assertThat(a.tongSao()).isEqualTo(4);
        assertThat(a.soLuotYeuThich()).isEqualTo(1);
        assertThat(goiYRepository.danhGiaCuaHang(tu)[0]).isPositive();
        assertThat(cuaHangRepository.timTheTheoMa(List.of(spA, spHet, spTat)))
            .extracting(CuaHang_Repository.TheSanPhamCoMa::getMaSanPham).containsExactly(spA);
    }

    private long id(String sql, Object... params) {
        return jdbc.sql(sql).params(params).query(Long.class).single();
    }

    private long sanPham(long th, long dm, String slug) {
        return id("INSERT INTO san_pham (ma_thuong_hieu, ma_danh_muc, ten_san_pham, duong_dan) VALUES (?, ?, ?, ?) RETURNING ma_san_pham",
            th, dm, "Sản phẩm " + slug, "gy-" + slug);
    }

    private long bienThe(long sp, String sku, int ton, int giu) {
        return id("""
            INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho, so_luong_tam_giu)
            VALUES (?, ?, '30 ml', 100000, ?, ?) RETURNING ma_bien_the""", sp, sku, ton, giu);
    }

    private long don(long khach, String ma, String trangThai, int soNgayTruoc) {
        return id("""
            INSERT INTO don_hang (ma_don_hang_hien_thi, ma_nguoi_dung, ten_nguoi_nhan, sdt_nguoi_nhan, dia_chi_khi_dat,
                tam_tinh, tong_thanh_toan, trang_thai, phuong_thuc_thanh_toan, ngay_tao)
            VALUES (?, ?, 'N', '0900000000', 'Đ', 100000, 125000, ?, 'COD', UTC_TIMESTAMP(6) - INTERVAL ? DAY) RETURNING ma_don_hang""",
            ma, khach, trangThai, soNgayTruoc);
    }

    private long dong(long don, long bt, int soLuong) {
        return id("""
            INSERT INTO chi_tiet_don_hang (ma_don_hang, ma_bien_the, sku_khi_dat, ten_san_pham_khi_dat, ten_bien_the_khi_dat, don_gia, so_luong)
            VALUES (?, ?, 'SKU', 'SP', '30 ml', 100000, ?) RETURNING ma_chi_tiet_don_hang""", don, bt, soLuong);
    }

    private void danhGia(long khach, long sp, long ct, int sao, boolean hienThi, int soNgayTruoc) {
        jdbc.sql("""
            INSERT INTO danh_gia (ma_nguoi_dung, ma_san_pham, ma_chi_tiet_don_hang, so_sao, hien_thi, ngay_tao)
            VALUES (?, ?, ?, ?, ?, UTC_TIMESTAMP(6) - INTERVAL ? DAY)""").params(khach, sp, ct, sao, hienThi, soNgayTruoc).update();
    }
}
