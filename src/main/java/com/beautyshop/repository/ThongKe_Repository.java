package com.beautyshop.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Truy vấn tổng hợp chỉ đọc cho Dashboard Admin. Các cột thời gian đơn hàng, thanh toán, đánh giá, đổi trả
 * và voucher lưu theo UTC; {@code nguoi_dung.ngay_tao} dùng mặc định CURRENT_TIMESTAMP của MariaDB (giờ máy chủ),
 * nên caller truyền mốc giờ địa phương cho truy vấn khách hàng mới.
 */
@Repository
public class ThongKe_Repository {
    /**
     * Đơn đã thu tiền: có đúng một lần thanh toán PAID khớp tổng đơn (lấy mốc thu sớm nhất), đơn không ở CANCELLED/EXPIRED.
     * Thanh toán đang hoàn (REFUND_PENDING/REFUNDED) không phải PAID nên tự loại.
     */
    static final String DON_DA_THU = """
        WITH don_da_thu AS (
            SELECT d.ma_don_hang, d.tong_thanh_toan, d.phuong_thuc_thanh_toan, MIN(tt.ngay_thanh_toan) AS ngay_thu
            FROM don_hang d
            JOIN lan_thanh_toan tt ON tt.ma_don_hang = d.ma_don_hang
            WHERE tt.trang_thai = 'PAID' AND tt.so_tien = d.tong_thanh_toan AND tt.ngay_thanh_toan IS NOT NULL
              AND d.trang_thai NOT IN ('CANCELLED', 'EXPIRED')
            GROUP BY d.ma_don_hang, d.tong_thanh_toan, d.phuong_thuc_thanh_toan
        )
        """;
    /** Đơn đã đặt thành công: loại đơn online chưa thanh toán hoặc đã hết hạn giữ hàng. */
    static final String DON_DA_DAT = "trang_thai NOT IN ('PENDING_PAYMENT', 'EXPIRED')";
    public static final List<String> TRANG_THAI_CHO_XU_LY = List.of("PENDING_CONFIRMATION", "CONFIRMED", "PACKING");

    /** Số giờ lệch UTC của múi giờ báo cáo Asia/Ho_Chi_Minh (+7, không có giờ mùa hè). */
    static final int LECH_GIO_VIET_NAM = 7;

    private final JdbcClient jdbc;

    public ThongKe_Repository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static String ngayBaoCao(String cot) {
        return "CAST(TIMESTAMPADD(HOUR, " + LECH_GIO_VIET_NAM + ", " + cot + ") AS DATE)";
    }

    /** Doanh thu và số đơn đã thu cho kỳ [a, b) và kỳ trước [pa, a), mốc UTC. */
    public DoanhThu doanhThu(LocalDateTime pa, LocalDateTime a, LocalDateTime b) {
        return jdbc.sql(DON_DA_THU + """
            SELECT COALESCE(SUM(CASE WHEN ngay_thu >= :a THEN tong_thanh_toan END), 0),
                   COUNT(CASE WHEN ngay_thu >= :a THEN 1 END),
                   COALESCE(SUM(CASE WHEN ngay_thu < :a THEN tong_thanh_toan END), 0),
                   COUNT(CASE WHEN ngay_thu < :a THEN 1 END)
            FROM don_da_thu WHERE ngay_thu >= :pa AND ngay_thu < :b
            """).param("pa", pa).param("a", a).param("b", b)
            .query((rs, row) -> new DoanhThu(rs.getBigDecimal(1), rs.getLong(2), rs.getBigDecimal(3), rs.getLong(4))).single();
    }

    /** Số đơn đã đặt và số đơn hủy (theo ngày tạo) cho kỳ hiện tại và kỳ trước, mốc UTC. */
    public DemDon demDon(LocalDateTime pa, LocalDateTime a, LocalDateTime b) {
        return jdbc.sql("""
            SELECT COUNT(CASE WHEN ngay_tao >= :a THEN 1 END),
                   COUNT(CASE WHEN ngay_tao >= :a AND trang_thai = 'CANCELLED' THEN 1 END),
                   COUNT(CASE WHEN ngay_tao < :a THEN 1 END),
                   COUNT(CASE WHEN ngay_tao < :a AND trang_thai = 'CANCELLED' THEN 1 END)
            FROM don_hang WHERE ngay_tao >= :pa AND ngay_tao < :b AND
            """ + DON_DA_DAT).param("pa", pa).param("a", a).param("b", b)
            .query((rs, row) -> new DemDon(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4))).single();
    }

    /** Khách hàng mới theo mốc giờ địa phương của máy chủ CSDL (cột dùng CURRENT_TIMESTAMP). */
    public long[] khachMoi(LocalDateTime pa, LocalDateTime a, LocalDateTime b) {
        return jdbc.sql("""
            SELECT COUNT(CASE WHEN ngay_tao >= :a THEN 1 END), COUNT(CASE WHEN ngay_tao < :a THEN 1 END)
            FROM nguoi_dung WHERE vai_tro = 'CUSTOMER' AND ngay_tao >= :pa AND ngay_tao < :b
            """).param("pa", pa).param("a", a).param("b", b)
            .query((rs, row) -> new long[] {rs.getLong(1), rs.getLong(2)}).single();
    }

    /** Số liệu hiện tại (không theo kỳ): đơn chờ xử lý, đổi trả chờ, voucher sắp hết hạn. Đánh giá chờ duyệt: DanhGia_Store (P23). */
    public HienTai hienTai(LocalDateTime nowUtc) {
        return jdbc.sql("""
            SELECT (SELECT COUNT(*) FROM don_hang WHERE trang_thai IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'PACKING')),
                   (SELECT COUNT(*) FROM yeu_cau_doi_tra WHERE trang_thai = 'PENDING'),
                   (SELECT COUNT(*) FROM phieu_giam_gia WHERE dang_hoat_dong = TRUE
                        AND ket_thuc_luc > :now AND ket_thuc_luc <= :han)
            """).param("now", nowUtc).param("han", nowUtc.plusDays(7))
            .query((rs, row) -> new HienTai(rs.getLong(1), rs.getLong(2), rs.getLong(3))).single();
    }

    public List<Ngay> doanhThuTheoNgay(LocalDateTime a, LocalDateTime b) {
        String ngay = ngayBaoCao("ngay_thu");
        return jdbc.sql(DON_DA_THU + "SELECT " + ngay + " AS ngay, COALESCE(SUM(tong_thanh_toan), 0), COUNT(*)"
            + " FROM don_da_thu WHERE ngay_thu >= :a AND ngay_thu < :b GROUP BY " + ngay + " ORDER BY ngay").param("a", a).param("b", b)
            .query((rs, row) -> new Ngay(rs.getObject(1, LocalDate.class), rs.getBigDecimal(2), rs.getLong(3))).list();
    }

    public List<Ngay> donTheoNgay(LocalDateTime a, LocalDateTime b) {
        String ngay = ngayBaoCao("ngay_tao");
        return jdbc.sql("SELECT " + ngay + " AS ngay, 0, COUNT(*) FROM don_hang WHERE ngay_tao >= :a AND ngay_tao < :b AND "
            + DON_DA_DAT + " GROUP BY " + ngay + " ORDER BY ngay").param("a", a).param("b", b)
            .query((rs, row) -> new Ngay(rs.getObject(1, LocalDate.class), BigDecimal.ZERO, rs.getLong(3))).list();
    }

    /** Mọi đơn tạo trong kỳ theo trạng thái (kể cả chờ thanh toán/hết hạn để thấy toàn cảnh). */
    public List<Nhom> donTheoTrangThai(LocalDateTime a, LocalDateTime b) {
        return jdbc.sql("""
            SELECT trang_thai, COUNT(*), COALESCE(SUM(tong_thanh_toan), 0) FROM don_hang
            WHERE ngay_tao >= :a AND ngay_tao < :b GROUP BY trang_thai
            """).param("a", a).param("b", b)
            .query((rs, row) -> new Nhom(rs.getString(1), rs.getLong(2), rs.getBigDecimal(3))).list();
    }

    /** Top sản phẩm theo số lượng trong các đơn đã thu tiền trong kỳ; doanh thu dòng = đơn giá × SL (trước voucher, phí ship). */
    public List<Nhom> topSanPham(LocalDateTime a, LocalDateTime b, int gioiHan) {
        return jdbc.sql(DON_DA_THU + """
            SELECT MAX(sp.ten_san_pham), SUM(ct.so_luong), SUM(ct.don_gia * ct.so_luong)
            FROM don_da_thu x
            JOIN chi_tiet_don_hang ct ON ct.ma_don_hang = x.ma_don_hang
            JOIN bien_the_san_pham bt ON bt.ma_bien_the = ct.ma_bien_the
            JOIN san_pham sp ON sp.ma_san_pham = bt.ma_san_pham
            WHERE x.ngay_thu >= :a AND x.ngay_thu < :b
            GROUP BY sp.ma_san_pham
            ORDER BY SUM(ct.so_luong) DESC, SUM(ct.don_gia * ct.so_luong) DESC, sp.ma_san_pham
            LIMIT :n
            """).param("a", a).param("b", b).param("n", gioiHan)
            .query((rs, row) -> new Nhom(rs.getString(1), rs.getLong(2), rs.getBigDecimal(3))).list();
    }

    public List<Nhom> doanhThuTheoDanhMuc(LocalDateTime a, LocalDateTime b) {
        return jdbc.sql(DON_DA_THU + """
            SELECT MAX(dm.ten_danh_muc), SUM(ct.so_luong), SUM(ct.don_gia * ct.so_luong)
            FROM don_da_thu x
            JOIN chi_tiet_don_hang ct ON ct.ma_don_hang = x.ma_don_hang
            JOIN bien_the_san_pham bt ON bt.ma_bien_the = ct.ma_bien_the
            JOIN san_pham sp ON sp.ma_san_pham = bt.ma_san_pham
            JOIN danh_muc dm ON dm.ma_danh_muc = sp.ma_danh_muc
            WHERE x.ngay_thu >= :a AND x.ngay_thu < :b
            GROUP BY dm.ma_danh_muc
            ORDER BY SUM(ct.don_gia * ct.so_luong) DESC, dm.ma_danh_muc
            """).param("a", a).param("b", b)
            .query((rs, row) -> new Nhom(rs.getString(1), rs.getLong(2), rs.getBigDecimal(3))).list();
    }

    public List<Nhom> phuongThucThanhToan(LocalDateTime a, LocalDateTime b) {
        return jdbc.sql(DON_DA_THU + """
            SELECT phuong_thuc_thanh_toan, COUNT(*), COALESCE(SUM(tong_thanh_toan), 0)
            FROM don_da_thu WHERE ngay_thu >= :a AND ngay_thu < :b
            GROUP BY phuong_thuc_thanh_toan
            """).param("a", a).param("b", b)
            .query((rs, row) -> new Nhom(rs.getString(1), rs.getLong(2), rs.getBigDecimal(3))).list();
    }

    public List<DonMoi> donMoiNhat(int gioiHan) {
        return jdbc.sql("""
            SELECT d.ma_don_hang, d.ma_don_hang_hien_thi, n.ho_ten, d.tong_thanh_toan, d.trang_thai, d.ngay_tao
            FROM don_hang d JOIN nguoi_dung n ON n.ma_nguoi_dung = d.ma_nguoi_dung
            ORDER BY d.ngay_tao DESC, d.ma_don_hang DESC LIMIT :n
            """).param("n", gioiHan)
            .query((rs, row) -> new DonMoi(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBigDecimal(4),
                rs.getString(5), rs.getObject(6, LocalDateTime.class))).list();
    }

    /** SKU đang bán có tồn khả dụng (trong kho − tạm giữ) không vượt ngưỡng. */
    public List<SkuSapHet> skuSapHet(int nguong, int gioiHan) {
        return jdbc.sql("""
            SELECT bt.ma_bien_the, bt.ma_san_pham, bt.sku, sp.ten_san_pham, bt.ten_bien_the,
                   bt.so_luong_trong_kho - bt.so_luong_tam_giu AS kha_dung
            FROM bien_the_san_pham bt JOIN san_pham sp ON sp.ma_san_pham = bt.ma_san_pham
            WHERE bt.dang_hoat_dong = TRUE AND sp.dang_hoat_dong = TRUE
              AND bt.so_luong_trong_kho - bt.so_luong_tam_giu <= :nguong
            ORDER BY kha_dung, bt.sku LIMIT :n
            """).param("nguong", nguong).param("n", gioiHan)
            .query((rs, row) -> new SkuSapHet(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                rs.getString(5), rs.getInt(6))).list();
    }

    public record DoanhThu(BigDecimal hienTai, long soDonThu, BigDecimal kyTruoc, long soDonThuKyTruoc) {}
    public record DemDon(long soDon, long soHuy, long soDonKyTruoc, long soHuyKyTruoc) {}
    public record HienTai(long choXuLy, long doiTraCho, long voucherSapHetHan) {}
    public record Ngay(LocalDate ngay, BigDecimal doanhThu, long soDon) {}
    public record Nhom(String nhan, long soLuong, BigDecimal soTien) {}
    public record DonMoi(long maDonHang, String maHienThi, String khachHang, BigDecimal tong, String trangThai,
                         LocalDateTime ngayTaoUtc) {}
    public record SkuSapHet(long maBienThe, long maSanPham, String sku, String tenSanPham, String tenBienThe, int khaDung) {}
}
