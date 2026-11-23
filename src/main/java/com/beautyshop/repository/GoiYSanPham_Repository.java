package com.beautyshop.repository;

import com.beautyshop.recommendation.DiemPhoBien_Policy.UngVien;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Số liệu đầu vào cho gợi ý phổ biến, chỉ đọc. Ứng viên = sản phẩm đang bán (sản phẩm, thương hiệu, danh mục active)
 * có ít nhất một SKU active còn khả dụng — cùng điều kiện với dải nổi bật trang chủ.
 * Mốc {@code tuUtc} so với các cột UTC ({@code don_hang.ngay_tao}, {@code danh_gia.ngay_tao}).
 */
@Repository
public class GoiYSanPham_Repository {
    /** Đơn được coi là đã mua: loại đơn hủy, đơn online hết hạn và đơn còn chờ thanh toán. */
    static final String DON_KHONG_HUY = "d.trang_thai NOT IN ('CANCELLED', 'EXPIRED', 'PENDING_PAYMENT')";

    private final JdbcClient jdbc;

    public GoiYSanPham_Repository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<UngVien> ungVien(LocalDateTime tuUtc) {
        return jdbc.sql("""
            SELECT sp.ma_san_pham, sp.ngay_tao, COALESCE(ban.so_luong, 0), COALESCE(dg.so_danh_gia, 0),
                   COALESCE(dg.tong_sao, 0), COALESCE(yt.so_luot, 0)
            FROM san_pham sp
            JOIN thuong_hieu th ON th.ma_thuong_hieu = sp.ma_thuong_hieu
            JOIN danh_muc dm ON dm.ma_danh_muc = sp.ma_danh_muc
            LEFT JOIN (
                SELECT bt.ma_san_pham, SUM(ct.so_luong) AS so_luong
                FROM chi_tiet_don_hang ct
                JOIN don_hang d ON d.ma_don_hang = ct.ma_don_hang
                JOIN bien_the_san_pham bt ON bt.ma_bien_the = ct.ma_bien_the
                WHERE d.ngay_tao >= :tu AND
            """ + DON_KHONG_HUY + """

                GROUP BY bt.ma_san_pham
            ) ban ON ban.ma_san_pham = sp.ma_san_pham
            LEFT JOIN (
                SELECT ma_san_pham, COUNT(*) AS so_danh_gia, SUM(so_sao) AS tong_sao
                FROM danh_gia WHERE hien_thi = TRUE AND ngay_tao >= :tu
                GROUP BY ma_san_pham
            ) dg ON dg.ma_san_pham = sp.ma_san_pham
            LEFT JOIN (
                SELECT ma_san_pham, COUNT(*) AS so_luot FROM san_pham_yeu_thich GROUP BY ma_san_pham
            ) yt ON yt.ma_san_pham = sp.ma_san_pham
            WHERE sp.dang_hoat_dong = TRUE AND th.dang_hoat_dong = TRUE AND dm.dang_hoat_dong = TRUE
              AND EXISTS (SELECT 1 FROM bien_the_san_pham b WHERE b.ma_san_pham = sp.ma_san_pham
                          AND b.dang_hoat_dong = TRUE AND b.so_luong_trong_kho - b.so_luong_tam_giu > 0)
            """).param("tu", tuUtc)
            .query((rs, row) -> new UngVien(rs.getLong(1), rs.getObject(2, LocalDateTime.class), rs.getLong(3),
                rs.getLong(4), rs.getLong(5), rs.getLong(6))).list();
    }

    /** {số đánh giá, tổng sao} toàn cửa hàng trong kỳ, chỉ đánh giá đã hiển thị. */
    public long[] danhGiaCuaHang(LocalDateTime tuUtc) {
        return jdbc.sql("SELECT COUNT(*), COALESCE(SUM(so_sao), 0) FROM danh_gia WHERE hien_thi = TRUE AND ngay_tao >= :tu")
            .param("tu", tuUtc).query((rs, row) -> new long[] {rs.getLong(1), rs.getLong(2)}).single();
    }
}
