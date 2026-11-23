package com.beautyshop.service;

import com.beautyshop.dto.BoLocThongKe_Form;
import com.beautyshop.repository.ThongKe_Repository;
import com.beautyshop.service.ThongKe_Service.Huong;
import com.beautyshop.service.ThongKe_Service.Kpi;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;
import org.springframework.validation.BeanPropertyBindingResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chạy thật truy vấn thống kê trên H2. Bảng đơn hàng/thanh toán không phải entity nên test tạo bảng tối thiểu
 * với đúng tên cột của V1. "Hôm nay" cố định 2026-11-23 12:00 giờ Việt Nam.
 */
@DataJpaTest
@TestPropertySource(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class ThongKeServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-23T05:00:00Z"), ZoneOffset.UTC);

    @Autowired JdbcClient jdbc;
    ThongKe_Service service;

    @BeforeEach
    void setUp() {
        service = new ThongKe_Service(new ThongKe_Repository(jdbc), CLOCK);
        jdbc.sql("ALTER TABLE nguoi_dung ADD COLUMN IF NOT EXISTS ngay_tao TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP").update();
        jdbc.sql("""
            CREATE TABLE IF NOT EXISTS don_hang (ma_don_hang BIGINT PRIMARY KEY, ma_don_hang_hien_thi VARCHAR(80) NOT NULL,
                ma_nguoi_dung BIGINT NOT NULL, tong_thanh_toan DECIMAL(15,0) NOT NULL, trang_thai VARCHAR(30) NOT NULL,
                phuong_thuc_thanh_toan VARCHAR(20) NOT NULL, ngay_tao TIMESTAMP(6) NOT NULL)""").update();
        jdbc.sql("""
            CREATE TABLE IF NOT EXISTS lan_thanh_toan (ma_lan_thanh_toan BIGINT AUTO_INCREMENT PRIMARY KEY, ma_don_hang BIGINT NOT NULL,
                so_tien DECIMAL(15,0) NOT NULL, trang_thai VARCHAR(30) NOT NULL, ngay_thanh_toan TIMESTAMP(6))""").update();
        jdbc.sql("""
            CREATE TABLE IF NOT EXISTS chi_tiet_don_hang (ma_chi_tiet_don_hang BIGINT AUTO_INCREMENT PRIMARY KEY,
                ma_don_hang BIGINT NOT NULL, ma_bien_the BIGINT NOT NULL, don_gia DECIMAL(15,0) NOT NULL, so_luong INT NOT NULL)""").update();
        jdbc.sql("""
            CREATE TABLE IF NOT EXISTS danh_gia (ma_danh_gia BIGINT AUTO_INCREMENT PRIMARY KEY, so_sao INT NOT NULL,
                hien_thi BOOLEAN NOT NULL, ngay_tao TIMESTAMP(6) NOT NULL)""").update();
        jdbc.sql("CREATE TABLE IF NOT EXISTS yeu_cau_doi_tra (ma_yeu_cau_doi_tra BIGINT AUTO_INCREMENT PRIMARY KEY, trang_thai VARCHAR(30) NOT NULL)").update();
        jdbc.sql("""
            CREATE TABLE IF NOT EXISTS phieu_giam_gia (ma_phieu_giam_gia BIGINT AUTO_INCREMENT PRIMARY KEY,
                dang_hoat_dong BOOLEAN NOT NULL, ket_thuc_luc TIMESTAMP(6) NOT NULL)""").update();
    }

    @Test
    void doanhThuLoaiDonHuyVaSoSanhKyTruocTheoGioVietNam() {
        catalog();
        nguoiDung(1, "CUSTOMER", "2026-11-20 09:00:00");
        nguoiDung(2, "CUSTOMER", "2026-11-12 09:00:00");
        nguoiDung(3, "ADMIN", "2026-11-20 09:00:00");
        // Kỳ hiện tại 17–23/11 (giờ VN). Đơn 1: COD đã giao, thu 20/11.
        don(1, "DELIVERED", "COD", 300000, "2026-11-18 02:00:00");
        thanhToan(1, 300000, "PAID", "2026-11-20 03:00:00");
        dong(1, 1, 100000, 2);
        // Đơn 2: đã hủy dù có lần thanh toán PAID cũ -> không vào doanh thu, có vào tỉ lệ hủy.
        don(2, "CANCELLED", "MOMO", 500000, "2026-11-19 02:00:00");
        thanhToan(2, 500000, "PAID", "2026-11-19 02:05:00");
        dong(2, 1, 250000, 2);
        // Đơn 3: 16/11 17:30 UTC = 17/11 00:30 giờ VN -> thuộc kỳ hiện tại, ngày 17/11.
        don(3, "CONFIRMED", "MOMO", 200000, "2026-11-16 17:30:00");
        thanhToan(3, 200000, "PAID", "2026-11-16 17:31:00");
        dong(3, 2, 200000, 1);
        // Đơn 4: kỳ trước 10–16/11.
        don(4, "DELIVERED", "COD", 250000, "2026-11-11 02:00:00");
        thanhToan(4, 250000, "PAID", "2026-11-12 02:00:00");
        // Đơn 5: chờ thanh toán online -> không tính "Số đơn"; lần thanh toán FAILED không tính doanh thu.
        don(5, "PENDING_PAYMENT", "MOMO", 900000, "2026-11-22 02:00:00");
        thanhToan(5, 900000, "FAILED", null);
        // Đơn 6: thanh toán đang hoàn tiền -> không phải PAID.
        don(6, "DELIVERED", "MOMO", 120000, "2026-11-21 02:00:00");
        thanhToan(6, 120000, "REFUNDED", "2026-11-21 02:01:00");
        jdbc.sql("INSERT INTO danh_gia (so_sao, hien_thi, ngay_tao) VALUES (5, TRUE, TIMESTAMP '2026-11-21 01:00:00'), (4, FALSE, TIMESTAMP '2026-11-22 01:00:00')").update();
        jdbc.sql("INSERT INTO yeu_cau_doi_tra (trang_thai) VALUES ('PENDING'), ('COMPLETED')").update();
        jdbc.sql("INSERT INTO phieu_giam_gia (dang_hoat_dong, ket_thuc_luc) VALUES (TRUE, TIMESTAMP '2026-11-25 00:00:00'), (TRUE, TIMESTAMP '2026-12-31 00:00:00'), (FALSE, TIMESTAMP '2026-11-25 00:00:00')").update();

        var form = new BoLocThongKe_Form();
        form.setRange("7d");
        var errors = new BeanPropertyBindingResult(form, "boLoc");
        var ky = service.xacDinhKy(form, errors);
        assertThat(errors.hasErrors()).isFalse();
        assertThat(ky.tu()).isEqualTo(LocalDate.of(2026, 11, 17));
        assertThat(ky.kyTruoc().tu()).isEqualTo(LocalDate.of(2026, 11, 10));

        var tq = service.tongQuan(ky, 5);
        Kpi doanhThu = kpi(tq, "doanh-thu");
        assertThat(doanhThu.giaTri()).isEqualByComparingTo("500000");
        assertThat(doanhThu.kyTruoc()).isEqualByComparingTo("250000");
        assertThat(doanhThu.huong()).isEqualTo(Huong.TANG);
        assertThat(doanhThu.thayDoi()).isEqualByComparingTo("100.0");
        assertThat(kpi(tq, "so-don").giaTri()).isEqualByComparingTo("4");
        assertThat(kpi(tq, "aov").giaTri()).isEqualByComparingTo("250000");
        assertThat(kpi(tq, "aov").huong()).isEqualTo(Huong.KHONG_DOI);
        assertThat(kpi(tq, "khach-moi").giaTri()).isEqualByComparingTo("1");
        Kpi huy = kpi(tq, "ti-le-huy");
        assertThat(huy.giaTri()).isEqualByComparingTo("25.0");
        assertThat(huy.thayDoi()).isEqualByComparingTo("25.0");
        assertThat(huy.huong()).isEqualTo(Huong.TANG);

        assertThat(tq.theoNgay()).hasSize(7);
        assertThat(tq.theoNgay().get(0).doanhThu()).isEqualByComparingTo("200000");
        assertThat(tq.theoNgay().get(0).soDon()).isEqualTo(1);
        assertThat(tq.theoNgay().get(3).doanhThu()).isEqualByComparingTo("300000");
        assertThat(tq.topSanPham()).extracting(ThongKe_Repository.Nhom::nhan).containsExactly("Sản phẩm A", "Sản phẩm B");
        assertThat(tq.topSanPham().get(0).soLuong()).isEqualTo(2);
        assertThat(tq.theoDanhMuc()).singleElement().satisfies(n -> assertThat(n.soTien()).isEqualByComparingTo("400000"));
        assertThat(tq.theoPhuongThuc()).extracting(ThongKe_Repository.Nhom::soLuong).containsExactly(1L, 1L);
        assertThat(tq.theoSao()).extracting(ThongKe_Repository.Nhom::soLuong).containsExactly(1L, 1L, 0L, 0L, 0L);
        assertThat(tq.donChoXuLy()).isEqualTo(1);
        assertThat(tq.danhGiaChoDuyet()).isEqualTo(1);
        assertThat(tq.doiTraCho()).isEqualTo(1);
        assertThat(tq.voucherSapHetHan()).isEqualTo(1);
        assertThat(tq.donMoi()).hasSize(6);
        assertThat(tq.donMoi().get(0).maHienThi()).isEqualTo("BS-5");
        assertThat(tq.skuSapHet()).extracting(ThongKe_Repository.SkuSapHet::sku).containsExactly("SKU-2");
    }

    @Test
    void kyTrongHienThiTrangThaiRongVaKhongSoSanh() {
        var form = new BoLocThongKe_Form();
        form.setFrom(LocalDate.of(2025, 1, 1));
        form.setTo(LocalDate.of(2025, 1, 31));
        var errors = new BeanPropertyBindingResult(form, "boLoc");
        var tq = service.tongQuan(service.xacDinhKy(form, errors), 5);
        assertThat(errors.hasErrors()).isFalse();
        assertThat(tq.theoNgay()).hasSize(31);
        assertThat(tq.coDoanhThu()).isFalse();
        assertThat(tq.coDon()).isFalse();
        assertThat(tq.coTrangThai()).isFalse();
        assertThat(tq.topSanPham()).isEmpty();
        assertThat(kpi(tq, "doanh-thu").giaTri()).isEqualByComparingTo("0");
        assertThat(kpi(tq, "doanh-thu").huong()).isEqualTo(Huong.KHONG_SO_SANH);
        assertThat(kpi(tq, "ti-le-huy").giaTri()).isNull();
        assertThat(kpi(tq, "ti-le-huy").huong()).isEqualTo(Huong.KHONG_SO_SANH);
    }

    @Test
    void boLocSaiTraVeKyMacDinhKemLoi() {
        assertThat(loi(null, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1))).isEqualTo("ky.thuTu");
        assertThat(loi(null, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 24))).isEqualTo("ky.tuongLai");
        assertThat(loi(null, LocalDate.of(2025, 11, 1), LocalDate.of(2026, 11, 2))).isEqualTo("ky.dai");
        assertThat(loi(null, LocalDate.of(2025, 11, 1), null)).isEqualTo("ky.thieu");
        assertThat(loi("365d", null, null)).isEqualTo("ky.range");
        // 366 ngày tính cả hai đầu vẫn hợp lệ.
        assertThat(loi(null, LocalDate.of(2025, 11, 2), LocalDate.of(2026, 11, 2))).isNull();
    }

    private String loi(String range, LocalDate from, LocalDate to) {
        var form = new BoLocThongKe_Form();
        form.setRange(range);
        form.setFrom(from);
        form.setTo(to);
        var errors = new BeanPropertyBindingResult(form, "boLoc");
        var ky = service.xacDinhKy(form, errors);
        if (!errors.hasErrors()) return null;
        assertThat(ky.range()).isEqualTo("30d");
        assertThat(ky.den()).isEqualTo(LocalDate.of(2026, 11, 23));
        return errors.getGlobalError().getCode();
    }

    private static Kpi kpi(ThongKe_Service.TongQuan tq, String ma) {
        return tq.kpi().stream().filter(k -> k.ma().equals(ma)).findFirst().orElseThrow();
    }

    private void catalog() {
        jdbc.sql("INSERT INTO thuong_hieu (ma_thuong_hieu, ten_thuong_hieu, duong_dan, dang_hoat_dong) VALUES (1, 'TH', 'th', TRUE)").update();
        jdbc.sql("INSERT INTO danh_muc (ma_danh_muc, ten_danh_muc, duong_dan, dang_hoat_dong) VALUES (1, 'Dưỡng da', 'duong-da', TRUE)").update();
        for (long ma = 1; ma <= 2; ma++) {
            jdbc.sql("""
                INSERT INTO san_pham (ma_san_pham, ma_thuong_hieu, ma_danh_muc, ten_san_pham, duong_dan, dang_hoat_dong, ngay_tao)
                VALUES (?, 1, 1, ?, ?, TRUE, CURRENT_TIMESTAMP)""").params(ma, "Sản phẩm " + (ma == 1 ? "A" : "B"), "sp-" + ma).update();
            jdbc.sql("""
                INSERT INTO bien_the_san_pham (ma_bien_the, ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho, so_luong_tam_giu, dang_hoat_dong)
                VALUES (?, ?, ?, '30 ml', 100000, ?, ?, TRUE)""").params(ma, ma, "SKU-" + ma, ma == 1 ? 20 : 6, ma == 1 ? 0 : 2).update();
        }
    }

    private void nguoiDung(long ma, String vaiTro, String ngayTaoGioDiaPhuong) {
        jdbc.sql("""
            INSERT INTO nguoi_dung (ma_nguoi_dung, email, mat_khau_bam, ho_ten, vai_tro, kich_hoat, phien_ban_xac_thuc, ngay_tao)
            VALUES (?, ?, 'x', ?, ?, TRUE, 0, CAST(? AS TIMESTAMP))""")
            .params(ma, "u" + ma + "@example.test", "Khách " + ma, vaiTro, ngayTaoGioDiaPhuong).update();
    }

    private void don(long ma, String trangThai, String phuongThuc, int tong, String ngayTaoUtc) {
        jdbc.sql("INSERT INTO don_hang VALUES (?, ?, 1, ?, ?, ?, CAST(? AS TIMESTAMP))")
            .params(ma, "BS-" + ma, tong, trangThai, phuongThuc, ngayTaoUtc).update();
    }

    private void thanhToan(long don, int soTien, String trangThai, String ngayThanhToanUtc) {
        jdbc.sql("INSERT INTO lan_thanh_toan (ma_don_hang, so_tien, trang_thai, ngay_thanh_toan) VALUES (?, ?, ?, CAST(? AS TIMESTAMP))")
            .params(don, soTien, trangThai, ngayThanhToanUtc).update();
    }

    private void dong(long don, long bienThe, int donGia, int soLuong) {
        jdbc.sql("INSERT INTO chi_tiet_don_hang (ma_don_hang, ma_bien_the, don_gia, so_luong) VALUES (?, ?, ?, ?)")
            .params(don, bienThe, donGia, soLuong).update();
    }
}
