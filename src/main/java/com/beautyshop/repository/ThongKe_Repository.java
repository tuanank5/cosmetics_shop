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
    private static final String SKU_SAP_HET = """
        FROM bien_the_san_pham bt JOIN san_pham sp ON sp.ma_san_pham = bt.ma_san_pham
        WHERE bt.dang_hoat_dong = TRUE AND sp.dang_hoat_dong = TRUE
          AND bt.so_luong_trong_kho - bt.so_luong_tam_giu <= :nguong
        """;
    private static final String TOP_SAN_PHAM = """
        FROM don_da_thu x
        JOIN chi_tiet_don_hang ct ON ct.ma_don_hang = x.ma_don_hang
        JOIN bien_the_san_pham bt ON bt.ma_bien_the = ct.ma_bien_the
        JOIN san_pham sp ON sp.ma_san_pham = bt.ma_san_pham
        %s
        WHERE x.ngay_thu >= :a AND x.ngay_thu < :b
        GROUP BY sp.ma_san_pham
        ORDER BY SUM(ct.so_luong) DESC, SUM(ct.don_gia * ct.so_luong) DESC, sp.ma_san_pham
        LIMIT :n
        """;

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
            """ + TOP_SAN_PHAM.formatted("")).param("a", a).param("b", b).param("n", gioiHan)
            .query((rs, row) -> new Nhom(rs.getString(1), rs.getLong(2), rs.getBigDecimal(3))).list();
    }

    public List<SanPhamBanChay> topSanPhamChiTiet(LocalDateTime a, LocalDateTime b, int gioiHan) {
        return jdbc.sql(DON_DA_THU + """
            SELECT MAX(sp.ten_san_pham), SUM(ct.so_luong), SUM(ct.don_gia * ct.so_luong),
                   MAX(th.ten_thuong_hieu), MAX(sp.duong_dan), sp.ma_san_pham,
                   MAX(CASE WHEN sp.dang_hoat_dong AND th.dang_hoat_dong AND dm.dang_hoat_dong THEN 1 ELSE 0 END)
            """ + TOP_SAN_PHAM.formatted("""
                JOIN thuong_hieu th ON th.ma_thuong_hieu = sp.ma_thuong_hieu
                JOIN danh_muc dm ON dm.ma_danh_muc = sp.ma_danh_muc
                """))
            .param("a", a).param("b", b).param("n", gioiHan)
            .query((rs, row) -> new SanPhamBanChay(rs.getString(1), rs.getLong(2), rs.getBigDecimal(3),
                rs.getString(4), rs.getString(5), rs.getLong(6), rs.getBoolean(7))).list();
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
            """ + SKU_SAP_HET + """
            ORDER BY kha_dung, bt.sku LIMIT :n
            """).param("nguong", nguong).param("n", gioiHan)
            .query((rs, row) -> new SkuSapHet(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                rs.getString(5), rs.getInt(6))).list();
    }

    public long demSkuSapHet(int nguong) {
        return jdbc.sql("SELECT COUNT(*) " + SKU_SAP_HET).param("nguong", nguong).query(Long.class).single();
    }

    public DemGiao demDonDaGiao(LocalDateTime pa, LocalDateTime a, LocalDateTime b) {
        return jdbc.sql("""
            SELECT COUNT(CASE WHEN ngay_giao_thanh_cong >= :a THEN 1 END),
                   COUNT(CASE WHEN ngay_giao_thanh_cong < :a THEN 1 END)
            FROM don_hang WHERE ngay_giao_thanh_cong >= :pa AND ngay_giao_thanh_cong < :b
            """).param("pa", pa).param("a", a).param("b", b)
            .query((rs, row) -> new DemGiao(rs.getLong(1), rs.getLong(2))).single();
    }

    public TachDoanhThu tachDoanhThu(LocalDateTime a, LocalDateTime b) {
        return jdbc.sql(DON_DA_THU + """
            SELECT COALESCE(SUM(d.tam_tinh - d.so_tien_giam), 0), COALESCE(SUM(d.phi_van_chuyen), 0),
                   COUNT(CASE WHEN d.so_tien_giam > d.tam_tinh OR
                         d.tam_tinh - d.so_tien_giam + d.phi_van_chuyen <> x.tong_thanh_toan THEN 1 END)
            FROM don_da_thu x JOIN don_hang d ON d.ma_don_hang = x.ma_don_hang
            WHERE x.ngay_thu >= :a AND x.ngay_thu < :b
            """).param("a", a).param("b", b)
            .query((rs, row) -> new TachDoanhThu(rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getLong(3) == 0)).single();
    }

    public List<Nhom> doanhThuTheoThuongHieu(LocalDateTime a, LocalDateTime b) {
        return jdbc.sql(DON_DA_THU + """
            SELECT MAX(th.ten_thuong_hieu), SUM(ct.so_luong), SUM(ct.don_gia * ct.so_luong)
            FROM don_da_thu x
            JOIN chi_tiet_don_hang ct ON ct.ma_don_hang = x.ma_don_hang
            JOIN bien_the_san_pham bt ON bt.ma_bien_the = ct.ma_bien_the
            JOIN san_pham sp ON sp.ma_san_pham = bt.ma_san_pham
            JOIN thuong_hieu th ON th.ma_thuong_hieu = sp.ma_thuong_hieu
            WHERE x.ngay_thu >= :a AND x.ngay_thu < :b
            GROUP BY th.ma_thuong_hieu
            ORDER BY SUM(ct.don_gia * ct.so_luong) DESC, th.ma_thuong_hieu
            LIMIT 10
            """).param("a", a).param("b", b)
            .query((rs, row) -> new Nhom(rs.getString(1), rs.getLong(2), rs.getBigDecimal(3))).list();
    }

    /** Mỗi đơn tạo trong kỳ góp đúng một lần, kể cả đơn có nhiều lần thử MoMo. */
    public List<NhomThanhToan> trangThaiThanhToan(LocalDateTime a, LocalDateTime b, LocalDateTime now) {
        return jdbc.sql(DON_DA_THU + """
            , phan_loai AS (
                SELECT d.phuong_thuc_thanh_toan AS phuong_thuc,
                    CASE
                        WHEN d.phuong_thuc_thanh_toan = 'COD' AND d.trang_thai = 'CANCELLED' THEN 'KET_THUC'
                        WHEN x.ma_don_hang IS NOT NULL THEN 'DA_THU'
                        WHEN d.phuong_thuc_thanh_toan = 'COD' THEN 'DANG_CHO'
                        WHEN d.trang_thai IN ('EXPIRED', 'CANCELLED') THEN 'KET_THUC'
                        WHEN EXISTS (SELECT 1 FROM lan_thanh_toan l
                                WHERE l.ma_don_hang = d.ma_don_hang AND l.cong_thanh_toan IN ('MOMO', 'MOMO_MOCK')
                                  AND l.trang_thai IN ('REFUND_PENDING', 'REFUNDED') AND l.so_tien = d.tong_thanh_toan
                                  AND l.ngay_thanh_toan IS NOT NULL)
                             AND EXISTS (SELECT 1 FROM tam_giu_ton_kho t
                                WHERE t.ma_don_hang = d.ma_don_hang AND t.trang_thai = 'CONSUMED') THEN 'DA_THU'
                        WHEN d.trang_thai = 'PENDING_PAYMENT' AND %s THEN 'KET_THUC'
                        WHEN (SELECT l.trang_thai FROM lan_thanh_toan l
                              WHERE l.ma_don_hang = d.ma_don_hang AND l.cong_thanh_toan IN ('MOMO', 'MOMO_MOCK')
                              ORDER BY l.ma_lan_thanh_toan DESC LIMIT 1) IN ('FAILED', 'EXPIRED') THEN 'KET_THUC'
                        ELSE 'DANG_CHO'
                    END AS nhom
                FROM don_hang d LEFT JOIN don_da_thu x ON x.ma_don_hang = d.ma_don_hang
                WHERE d.ngay_tao >= :a AND d.ngay_tao < :b AND d.phuong_thuc_thanh_toan IN ('COD', 'MOMO')
            )
            SELECT phuong_thuc, COUNT(CASE WHEN nhom = 'DA_THU' THEN 1 END),
                   COUNT(CASE WHEN nhom = 'DANG_CHO' THEN 1 END), COUNT(CASE WHEN nhom = 'KET_THUC' THEN 1 END)
            FROM phan_loai GROUP BY phuong_thuc
            """.formatted(ThanhToanMomo_Repository.CO_GIU_HANG_QUA_HAN.formatted(":now")))
            .param("a", a).param("b", b).param("now", now)
            .query((rs, row) -> new NhomThanhToan(rs.getString(1), rs.getLong(2), rs.getLong(3), rs.getLong(4))).list();
    }

    public List<DoiTraCho> doiTraCho() {
        return jdbc.sql("""
            SELECT y.ma_yeu_cau_doi_tra, d.ma_don_hang_hien_thi, y.ly_do, y.ngay_yeu_cau
            FROM yeu_cau_doi_tra y JOIN don_hang d ON d.ma_don_hang = y.ma_don_hang
            WHERE y.trang_thai = 'PENDING'
            ORDER BY y.ngay_yeu_cau DESC, y.ma_yeu_cau_doi_tra DESC LIMIT 5
            """).query((rs, row) -> new DoiTraCho(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getObject(4, LocalDateTime.class))).list();
    }

    public List<CanhBaoThanhToan> canhBaoThanhToan(LocalDateTime a, LocalDateTime b, LocalDateTime now, String gateway) {
        return jdbc.sql("""
            SELECT d.ma_don_hang, d.ma_don_hang_hien_thi, l.so_tien, l.trang_thai,
                   COALESCE(l.ngay_thanh_toan, l.ngay_tao) AS thoi_diem
            FROM lan_thanh_toan l JOIN don_hang d ON d.ma_don_hang = l.ma_don_hang
            WHERE d.phuong_thuc_thanh_toan = 'MOMO' AND l.cong_thanh_toan IN ('MOMO', 'MOMO_MOCK') AND (
                (l.trang_thai = 'FAILED' AND l.ngay_tao >= :a AND l.ngay_tao < :b)
                OR (d.trang_thai = 'PENDING_PAYMENT' AND %s AND (
                    l.trang_thai = 'PENDING' OR l.cong_thanh_toan = :gateway
                ))
            )
            ORDER BY thoi_diem DESC, l.ma_lan_thanh_toan DESC
            """.formatted(ThanhToanMomo_Repository.CO_GIU_HANG_QUA_HAN.formatted(":now")))
            .param("a", a).param("b", b).param("now", now).param("gateway", gateway, java.sql.Types.VARCHAR)
            .query((rs, row) -> new CanhBaoThanhToan(rs.getLong(1), rs.getString(2), rs.getBigDecimal(3),
                rs.getString(4), rs.getObject(5, LocalDateTime.class))).list();
    }

    public List<VoucherSapHetHan> voucherSapHetHan(LocalDateTime now) {
        return jdbc.sql("""
            SELECT ma_phieu_giam_gia, ma_ap_dung, ket_thuc_luc, gioi_han_su_dung - so_luot_da_dung - so_luot_tam_giu
            FROM phieu_giam_gia WHERE dang_hoat_dong = TRUE AND ket_thuc_luc > :now AND ket_thuc_luc <= :han
            ORDER BY ket_thuc_luc, ma_phieu_giam_gia
            """).param("now", now).param("han", now.plusDays(7))
            .query((rs, row) -> new VoucherSapHetHan(rs.getLong(1), rs.getString(2),
                rs.getObject(3, LocalDateTime.class), rs.getObject(4, Integer.class))).list();
    }

    public record DoanhThu(BigDecimal hienTai, long soDonThu, BigDecimal kyTruoc, long soDonThuKyTruoc) {}
    public record DemDon(long soDon, long soHuy, long soDonKyTruoc, long soHuyKyTruoc) {}
    public record HienTai(long choXuLy, long doiTraCho, long voucherSapHetHan) {}
    public record Ngay(LocalDate ngay, BigDecimal doanhThu, long soDon) {}
    public record Nhom(String nhan, long soLuong, BigDecimal soTien) {}
    public record DonMoi(long maDonHang, String maHienThi, String khachHang, BigDecimal tong, String trangThai,
                         LocalDateTime ngayTaoUtc) {}
    public record SkuSapHet(long maBienThe, long maSanPham, String sku, String tenSanPham, String tenBienThe, int khaDung) {}
    public record DemGiao(long hienTai, long kyTruoc) {}
    public record TachDoanhThu(BigDecimal tienHang, BigDecimal phiVanChuyen, boolean khop) {}
    public record SanPhamBanChay(String nhan, long soLuong, BigDecimal soTien, String thuongHieu, String duongDan,
                                 long maSanPham, boolean dangHoatDong) {}
    public record NhomThanhToan(String phuongThuc, long daThu, long dangCho, long ketThuc) {}
    public record DoiTraCho(long maYeuCau, String maDon, String lyDo, LocalDateTime ngayYeuCauUtc) {}
    public record CanhBaoThanhToan(long maDonHang, String maDon, BigDecimal soTien, String trangThai, LocalDateTime thoiDiemUtc) {}
    public record VoucherSapHetHan(long maVoucher, String maApDung, LocalDateTime ketThucUtc, Integer luotConLai) {}
}
