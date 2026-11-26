package com.beautyshop.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class YeuThich_Repository {
    private static final String DANG_BAN = """
        s.dang_hoat_dong AND d.dang_hoat_dong AND t.dang_hoat_dong AND EXISTS
          (SELECT 1 FROM bien_the_san_pham b WHERE b.ma_san_pham = s.ma_san_pham AND b.dang_hoat_dong)
        """;

    private final JdbcClient jdbc;
    YeuThich_Repository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public List<Muc> danhSach(long maNguoiDung) {
        return jdbc.sql("SELECT s.ma_san_pham, s.ten_san_pham, s.duong_dan, (" + DANG_BAN + ") AS dang_ban " + """
            FROM san_pham_yeu_thich y JOIN san_pham s ON s.ma_san_pham = y.ma_san_pham
            JOIN danh_muc d ON d.ma_danh_muc = s.ma_danh_muc
            JOIN thuong_hieu t ON t.ma_thuong_hieu = s.ma_thuong_hieu
            WHERE y.ma_nguoi_dung = ? ORDER BY y.ma_yeu_thich DESC
            """).param(maNguoiDung).query((rs, row) -> new Muc(rs.getLong(1), rs.getString(2),
                rs.getString(3), rs.getBoolean(4))).list();
    }

    public void them(long maNguoiDung, long maSanPham) {
        jdbc.sql("""
            INSERT INTO san_pham_yeu_thich (ma_nguoi_dung, ma_san_pham)
            SELECT ?, s.ma_san_pham FROM san_pham s
            JOIN danh_muc d ON d.ma_danh_muc = s.ma_danh_muc
            JOIN thuong_hieu t ON t.ma_thuong_hieu = s.ma_thuong_hieu
            WHERE s.ma_san_pham = ? AND
            """ + DANG_BAN + " ON DUPLICATE KEY UPDATE ma_yeu_thich = ma_yeu_thich")
            .params(maNguoiDung, maSanPham).update();
    }

    public void xoa(long maNguoiDung, long maSanPham) {
        jdbc.sql("DELETE FROM san_pham_yeu_thich WHERE ma_nguoi_dung = ? AND ma_san_pham = ?")
            .params(maNguoiDung, maSanPham).update();
    }

    public Set<Long> daLuu(long maNguoiDung, Collection<Long> sanPham) {
        if (sanPham != null && sanPham.isEmpty()) return Set.of();
        var query = jdbc.sql("""
            SELECT ma_san_pham FROM san_pham_yeu_thich WHERE ma_nguoi_dung = :chu
            """ + (sanPham == null ? "" : " AND ma_san_pham IN (:sanPham)")).param("chu", maNguoiDung);
        if (sanPham != null) query.param("sanPham", sanPham);
        return query.query(Long.class).set();
    }

    public TrangThai trangThai(long maNguoiDung, long maSanPham) {
        return jdbc.sql("SELECT s.duong_dan, (" + DANG_BAN + "), y.ma_yeu_thich IS NOT NULL " + """
            FROM san_pham s
            JOIN danh_muc d ON d.ma_danh_muc = s.ma_danh_muc
            JOIN thuong_hieu t ON t.ma_thuong_hieu = s.ma_thuong_hieu
            LEFT JOIN san_pham_yeu_thich y ON y.ma_san_pham = s.ma_san_pham AND y.ma_nguoi_dung = ?
            WHERE s.ma_san_pham = ?
            """).params(maNguoiDung, maSanPham)
            .query((rs, row) -> new TrangThai(rs.getString(1), rs.getBoolean(2), rs.getBoolean(3)))
            .optional().orElse(new TrangThai(null, false, false));
    }

    public record Muc(long maSanPham, String tenSanPham, String slug, boolean dangBan) {}
    public record TrangThai(String slug, boolean dangBan, boolean daLuu) {}
}
