package com.beautyshop.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TriThuc_Repository {
    private final JdbcClient jdbc;
    TriThuc_Repository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public List<Faq> faqCongKhai() {
        return jdbc.sql("""
            SELECT ma_faq, cau_hoi, cau_tra_loi, url_nguon, ngay_cap_nhat
            FROM bai_viet_faq WHERE dang_hoat_dong = TRUE AND url_nguon IS NOT NULL
            ORDER BY ma_faq
            """).query((rs, row) -> new Faq(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getObject(5, LocalDateTime.class))).list();
    }

    public List<SanPham> sanPhamCongKhai() {
        return jdbc.sql("""
            SELECT s.duong_dan, s.ten_san_pham, t.ten_thuong_hieu, d.ten_danh_muc
            FROM san_pham s
            JOIN thuong_hieu t ON t.ma_thuong_hieu = s.ma_thuong_hieu
            JOIN danh_muc d ON d.ma_danh_muc = s.ma_danh_muc
            WHERE s.dang_hoat_dong AND t.dang_hoat_dong AND d.dang_hoat_dong
              AND EXISTS (SELECT 1 FROM bien_the_san_pham b WHERE b.ma_san_pham = s.ma_san_pham AND b.dang_hoat_dong)
            ORDER BY s.ma_san_pham
            """).query((rs, row) -> new SanPham(rs.getString(1), rs.getString(2),
                rs.getString(3), rs.getString(4))).list();
    }

    public record Faq(long ma, String cauHoi, String cauTraLoi, String url, LocalDateTime ngayCapNhat) {}
    public record SanPham(String slug, String ten, String thuongHieu, String danhMuc) {}
}
