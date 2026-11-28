package com.beautyshop.integration;

import com.beautyshop.dto.BoLocThongKe_Form;
import com.beautyshop.repository.ThongKe_Repository;
import com.beautyshop.service.ThongKe_Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BeanPropertyBindingResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P20: chạy thật các truy vấn thống kê trên MariaDB (CTE, TIMESTAMPADD/CAST, LIMIT tham số) và trang Dashboard.
 * Dữ liệu đặt ở tháng 3/2020 để không lẫn với dữ liệu của test khác trong cùng database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
class ThongKe_MariaDb_Test {
    @Autowired JdbcClient jdbc;
    @Autowired ThongKe_Service thongKeService;
    @Autowired ThongKe_Repository thongKeRepository;
    @Autowired MockMvc mvc;

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void doanhThuTheoNgayVietNamLoaiDonHuyVaCustomerBiChan() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long khach = jdbc.sql("""
            INSERT INTO nguoi_dung (email, mat_khau_bam, ho_ten, vai_tro, ngay_tao)
            VALUES (?, 'x', 'Khách thống kê', 'CUSTOMER', '2020-03-05 10:00:00') RETURNING ma_nguoi_dung
            """).param("tk-" + suffix + "@example.test").query(Long.class).single();
        long thuongHieu = jdbc.sql("INSERT INTO thuong_hieu (ten_thuong_hieu, duong_dan) VALUES ('TH thống kê', ?) RETURNING ma_thuong_hieu")
            .param("th-tk-" + suffix).query(Long.class).single();
        long danhMuc = jdbc.sql("INSERT INTO danh_muc (ten_danh_muc, duong_dan) VALUES (?, ?) RETURNING ma_danh_muc")
            .param("Danh mục " + suffix).param("dm-tk-" + suffix).query(Long.class).single();
        long sanPham = jdbc.sql("""
            INSERT INTO san_pham (ma_thuong_hieu, ma_danh_muc, ten_san_pham, duong_dan) VALUES (?, ?, ?, ?) RETURNING ma_san_pham
            """).param(thuongHieu).param(danhMuc).param("Sản phẩm " + suffix).param("sp-tk-" + suffix).query(Long.class).single();
        long bienThe = jdbc.sql("""
            INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho)
            VALUES (?, ?, '30 ml', 100000, 10) RETURNING ma_bien_the
            """).param(sanPham).param("TK-" + suffix).query(Long.class).single();

        // 2020-03-09 17:30 UTC = 2020-03-10 00:30 giờ VN -> ngày đầu kỳ 10–16/03.
        long don1 = don(suffix + "-1", khach, "DELIVERED", "COD", 225000, "2020-03-09 17:30:00");
        dong(don1, bienThe, "TK-" + suffix, 100000, 2);
        thanhToan(don1, "COD-TK-" + suffix, 225000, "PAID", "2020-03-09 17:40:00");
        long don2 = don(suffix + "-2", khach, "CANCELLED", "MOMO", 125000, "2020-03-12 03:00:00");
        dong(don2, bienThe, "TK-" + suffix, 100000, 1);
        thanhToan(don2, "MOMO-TK-" + suffix, 125000, "PAID", "2020-03-12 03:05:00");
        // Kỳ trước 03–09/03 (09/03 16:59 UTC vẫn là 09/03 23:59 giờ VN).
        long don3 = don(suffix + "-3", khach, "DELIVERED", "COD", 125000, "2020-03-09 16:59:00");
        dong(don3, bienThe, "TK-" + suffix, 100000, 1);
        thanhToan(don3, "COD-TK3-" + suffix, 125000, "PAID", "2020-03-09 16:59:00");
        // Đơn 3 đặt ở kỳ trước nhưng giao ở đúng đầu kỳ hiện tại. Đơn 1 giao sau kỳ hiện tại.
        jdbc.sql("UPDATE don_hang SET ngay_giao_thanh_cong = '2020-03-09 17:00:00' WHERE ma_don_hang = ?").param(don3).update();
        jdbc.sql("UPDATE don_hang SET ngay_giao_thanh_cong = '2020-03-16 17:00:00', tam_tinh = 210000, so_tien_giam = 10000 WHERE ma_don_hang = ?")
            .param(don1).update();
        jdbc.sql("UPDATE bien_the_san_pham SET so_luong_tam_giu = 5 WHERE ma_bien_the = ?").param(bienThe).update();

        long don4 = don(suffix + "-4", khach, "PENDING_PAYMENT", "MOMO", 125000, "2020-03-12 03:10:00");
        jdbc.sql("""
            INSERT INTO lan_thanh_toan (ma_don_hang, ma_yeu_cau, cong_thanh_toan, so_tien, trang_thai, ngay_tao)
            VALUES (?, ?, 'MOMO_MOCK', 125000, 'PENDING', '2020-03-12 03:10:00'),
                   (?, ?, 'MOMO_MOCK', 125000, 'FAILED', '2020-03-12 03:11:00')
            """).params(don4, "P20C-P-" + suffix, don4, "P20C-F-" + suffix).update();
        jdbc.sql("""
            INSERT INTO tam_giu_ton_kho (ma_don_hang, ma_bien_the, so_luong, het_han_luc, trang_thai)
            VALUES (?, ?, 1, '2020-03-12 03:25:00', 'HELD')
            """).params(don4, bienThe).update();
        jdbc.sql("""
            INSERT INTO yeu_cau_doi_tra (ma_don_hang, ma_nguoi_dung, trang_thai, ly_do, ngay_yeu_cau)
            VALUES (?, ?, 'PENDING', 'Lý do QA', '2020-03-14 09:00:00')
            """).params(don3, khach).update();
        jdbc.sql("""
            INSERT INTO phieu_giam_gia (ma_ap_dung, loai_giam_gia, gia_tri_giam, gioi_han_su_dung,
                so_luot_da_dung, so_luot_tam_giu, bat_dau_luc, ket_thuc_luc)
            VALUES (?, 'FIXED', 10000, 5, 1, 2, '2020-03-06 00:00:00', '2020-03-17 00:00:00')
            """).param("P20C-" + suffix).update();

        var form = new BoLocThongKe_Form();
        form.setFrom(LocalDate.of(2020, 3, 10));
        form.setTo(LocalDate.of(2020, 3, 16));

        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            "customer", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
        assertThatThrownBy(() -> thongKeService.homNay()).isInstanceOf(AccessDeniedException.class);

        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        var errors = new BeanPropertyBindingResult(form, "boLoc");
        var tq = thongKeService.tongQuan(thongKeService.xacDinhKy(form, errors), 5);
        assertThat(errors.hasErrors()).isFalse();
        var doanhThu = tq.kpi().get(0);
        assertThat(doanhThu.giaTri()).isEqualByComparingTo("225000");
        assertThat(doanhThu.kyTruoc()).isEqualByComparingTo("125000");
        assertThat(doanhThu.thayDoi()).isEqualByComparingTo("80.0");
        assertThat(tq.kpi().get(1).giaTri()).isEqualByComparingTo("2");
        assertThat(tq.kpi().get(4).giaTri()).isEqualByComparingTo("50.0");
        assertThat(tq.theoNgay().get(0).ngay()).isEqualTo(LocalDate.of(2020, 3, 10));
        assertThat(tq.theoNgay().get(0).doanhThu()).isEqualByComparingTo("225000");
        assertThat(tq.theoNgay().get(0).soDon()).isEqualTo(1);
        assertThat(tq.topSanPham()).extracting(ThongKe_Repository.Nhom::nhan).containsExactly("Sản phẩm " + suffix);
        assertThat(tq.topSanPham().get(0).soLuong()).isEqualTo(2);
        assertThat(tq.theoDanhMuc()).extracting(ThongKe_Repository.Nhom::nhan).containsExactly("Danh mục " + suffix);
        assertThat(tq.theoPhuongThuc().get(0).soLuong()).isEqualTo(1);
        assertThat(tq.theoPhuongThuc().get(1).soLuong()).isZero();
        assertThat(tq.donMoi()).isNotEmpty();
        assertThat(tq.chiSo("da-giao").giaTri()).isEqualByComparingTo("1");
        assertThat(tq.chiSo("da-giao").kyTruoc()).isEqualByComparingTo("0");
        var tatCaSkuSapHet = thongKeRepository.skuSapHet(5, Integer.MAX_VALUE);
        assertThat(tq.demSkuSapHet()).isEqualTo(tatCaSkuSapHet.size());
        assertThat(tq.skuSapHet()).containsExactlyElementsOf(tatCaSkuSapHet.stream().limit(20).toList());
        assertThat(tatCaSkuSapHet).anyMatch(sku -> sku.maBienThe() == bienThe && sku.khaDung() == 5);
        assertThat(tq.tachDoanhThu().khop()).isTrue();
        assertThat(tq.tachDoanhThu().tienHang()).isEqualByComparingTo("200000");
        assertThat(tq.tachDoanhThu().phiVanChuyen()).isEqualByComparingTo("25000");
        assertThat(tq.theoThuongHieu()).singleElement().satisfies(
            n -> assertThat(n.soTien()).isEqualByComparingTo("200000"));
        assertThat(tq.trangThaiThanhToan()).containsExactly(
            new ThongKe_Repository.NhomThanhToan("COD", 1, 0, 0),
            new ThongKe_Repository.NhomThanhToan("MOMO", 0, 0, 2));
        assertThat(tq.canhBaoThanhToan()).filteredOn(p -> p.maDonHang() == don4)
            .extracting(ThongKe_Repository.CanhBaoThanhToan::trangThai).containsExactly("FAILED", "PENDING");
        assertThat(tq.doiTraChoChiTiet()).anyMatch(r -> ("TK-" + suffix + "-3").equals(r.maDon()));
        assertThat(thongKeRepository.voucherSapHetHan(LocalDateTime.of(2020, 3, 13, 0, 0)))
            .anyMatch(v -> ("P20C-" + suffix).equals(v.maApDung()) && v.luotConLai() == 2);

        mvc.perform(get("/admin/orders/" + don1))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<a href=\"/admin/orders\" aria-current=\"page\">")));
        mvc.perform(get("/admin/catalog/brands"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<a href=\"/admin/catalog/brands\" aria-current=\"page\">")))
            .andExpect(content().string(not(containsString("<a href=\"/admin/catalog/categories\" aria-current=\"page\">"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void trangDashboardRenderTrenMariaDb() throws Exception {
        mvc.perform(get("/admin/dashboard").param("range", "90d"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Doanh thu thực thu")))
            .andExpect(content().string(containsString("id=\"table-daily\"")));
    }

    private long don(String ma, long khach, String trangThai, String phuongThuc, int tong, String ngayTaoUtc) {
        return jdbc.sql("""
            INSERT INTO don_hang (ma_don_hang_hien_thi, ma_nguoi_dung, ten_nguoi_nhan, sdt_nguoi_nhan, dia_chi_khi_dat,
                tam_tinh, phi_van_chuyen, tong_thanh_toan, trang_thai, phuong_thuc_thanh_toan, ngay_tao)
            VALUES (?, ?, 'Người nhận', '0900000000', 'Địa chỉ', ?, 25000, ?, ?, ?, ?) RETURNING ma_don_hang
            """).param("TK-" + ma).param(khach).param(tong - 25000).param(tong).param(trangThai).param(phuongThuc).param(ngayTaoUtc)
            .query(Long.class).single();
    }

    private void dong(long don, long bienThe, String sku, int gia, int soLuong) {
        jdbc.sql("""
            INSERT INTO chi_tiet_don_hang (ma_don_hang, ma_bien_the, sku_khi_dat, ten_san_pham_khi_dat, ten_bien_the_khi_dat, don_gia, so_luong)
            VALUES (?, ?, ?, 'SP', '30 ml', ?, ?)
            """).params(don, bienThe, sku, gia, soLuong).update();
    }

    private void thanhToan(long don, String maYeuCau, int soTien, String trangThai, String ngayThanhToanUtc) {
        jdbc.sql("""
            INSERT INTO lan_thanh_toan (ma_don_hang, ma_yeu_cau, cong_thanh_toan, so_tien, trang_thai, ngay_tao, ngay_thanh_toan)
            VALUES (?, ?, 'COD', ?, ?, ?, ?)
            """).params(don, maYeuCau, soTien, trangThai, ngayThanhToanUtc, ngayThanhToanUtc).update();
    }
}
