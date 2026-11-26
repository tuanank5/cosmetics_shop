package com.beautyshop.repository;

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

    public record MucMua(long maSanPham, String tenSanPham, String tenBienThe, long maDon, String trangThai,
                         long maBienThe, String tenHienThi) {}

    public record SanPhamLoc(long ma, String ten) {}

}
