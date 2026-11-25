package com.beautyshop.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Phần đánh giá luôn đọc từ MariaDB, dù nội dung đánh giá lưu ở store nào (P23):
 * điều kiện BR16 (chủ đơn, đơn DELIVERED, theo từng dòng đơn), tên sản phẩm theo lô cho Admin,
 * và nguồn dữ liệu cũ của bảng {@code danh_gia} cho bộ chuyển sang MongoDB.
 */
@Repository
public class DanhGia_Repository {
    private final JdbcClient jdbc;

    DanhGia_Repository(JdbcClient jdbc) { this.jdbc = jdbc; }

    /** Dòng đơn thuộc đúng người mua; trạng thái đơn để service kiểm DELIVERED. Không trả email/SĐT, chỉ tên rút gọn. */
    public Optional<MucMua> mucMua(long maMuc, long maNguoiDung) {
        return jdbc.sql("""
            SELECT b.ma_san_pham, ct.ten_san_pham_khi_dat, ct.ten_bien_the_khi_dat, d.ma_don_hang, d.trang_thai,
                   ct.ma_bien_the, n.ho_ten
            FROM chi_tiet_don_hang ct JOIN don_hang d ON d.ma_don_hang = ct.ma_don_hang
            JOIN bien_the_san_pham b ON b.ma_bien_the = ct.ma_bien_the
            JOIN nguoi_dung n ON n.ma_nguoi_dung = d.ma_nguoi_dung
            WHERE ct.ma_chi_tiet_don_hang = ? AND d.ma_nguoi_dung = ?
            """).params(maMuc, maNguoiDung).query((rs, row) -> new MucMua(rs.getLong(1), rs.getString(2),
                rs.getString(3), rs.getLong(4), rs.getString(5), rs.getLong(6), DanhGia_Store.tenRutGon(rs.getString(7))))
            .optional();
    }

    /** Tên sản phẩm theo lô (một truy vấn) cho danh sách Admin. */
    public Map<Long, String> tenSanPham(Collection<Long> ma) {
        Map<Long, String> ten = new HashMap<>();
        if (!ma.isEmpty()) {
            jdbc.sql("SELECT ma_san_pham, ten_san_pham FROM san_pham WHERE ma_san_pham IN (:ma)").param("ma", ma)
                .query((rs, row) -> ten.put(rs.getLong(1), rs.getString(2))).list();
        }
        return ten;
    }

    /** Lựa chọn sản phẩm cho bộ lọc Admin. */
    public List<SanPhamLoc> sanPhamLoc() {
        return jdbc.sql("SELECT ma_san_pham, ten_san_pham FROM san_pham ORDER BY ten_san_pham, ma_san_pham")
            .query((rs, row) -> new SanPhamLoc(rs.getLong(1), rs.getString(2))).list();
    }

    /** Toàn bộ bảng danh_gia cũ kèm snapshot đơn/biến thể/tên rút gọn, cho bộ chuyển sang MongoDB. */
    public List<DanhGiaCu> tatCaDanhGiaCu() {
        return jdbc.sql("""
            SELECT dg.ma_danh_gia, dg.ma_chi_tiet_don_hang, ct.ma_don_hang, dg.ma_nguoi_dung, dg.ma_san_pham,
                   ct.ma_bien_the, n.ho_ten, dg.so_sao, dg.noi_dung, dg.hien_thi, dg.ngay_tao
            FROM danh_gia dg
            JOIN chi_tiet_don_hang ct ON ct.ma_chi_tiet_don_hang = dg.ma_chi_tiet_don_hang
            JOIN nguoi_dung n ON n.ma_nguoi_dung = dg.ma_nguoi_dung
            ORDER BY dg.ma_danh_gia
            """).query((rs, row) -> new DanhGiaCu(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5),
                rs.getLong(6), DanhGia_Store.tenRutGon(rs.getString(7)), rs.getInt(8), rs.getString(9), rs.getBoolean(10),
                utc(rs.getObject(11, LocalDateTime.class)))).list();
    }

    static Instant utc(LocalDateTime gio) {
        return gio == null ? null : gio.toInstant(ZoneOffset.UTC);
    }

    public record MucMua(long maSanPham, String tenSanPham, String tenBienThe, long maDon, String trangThai,
                         long maBienThe, String tenHienThi) {}

    public record SanPhamLoc(long ma, String ten) {}

    /** {@code ngayTao} theo UTC (cột danh_gia.ngay_tao được ghi bằng UTC_TIMESTAMP). */
    public record DanhGiaCu(long maDanhGia, long maChiTietDonHang, long maDonHang, long maNguoiDung, long maSanPham,
                            long maBienThe, String tenHienThi, int soSao, String noiDung, boolean hienThi, Instant ngayTao) {}
}
