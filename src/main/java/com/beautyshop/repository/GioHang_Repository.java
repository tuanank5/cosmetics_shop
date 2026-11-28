package com.beautyshop.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class GioHang_Repository {
    private static final String DANG_HIEN_THI = "b.dang_hoat_dong AND s.dang_hoat_dong AND t.dang_hoat_dong AND d.dang_hoat_dong";
    private static final String COT_SKU = """
        s.ma_san_pham, s.ma_danh_muc, s.ma_thuong_hieu, b.ma_bien_the, s.ten_san_pham, b.ten_bien_the, b.gia_ban,
               CASE WHEN %1$s THEN b.so_luong_trong_kho - b.so_luong_tam_giu ELSE 0 END AS kha_dung, b.sku,
               b.url_hinh_anh, t.ten_thuong_hieu, CASE WHEN %1$s THEN s.duong_dan END AS duong_dan
        """.formatted(DANG_HIEN_THI);
    private static final String NGUON_SKU = """
        FROM bien_the_san_pham b
        JOIN san_pham s ON s.ma_san_pham = b.ma_san_pham
        JOIN thuong_hieu t ON t.ma_thuong_hieu = s.ma_thuong_hieu
        JOIN danh_muc d ON d.ma_danh_muc = s.ma_danh_muc
        """;

    private final JdbcClient jdbc;

    GioHang_Repository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<MucGio> timBienThe(long maBienThe, boolean khoa) {
        if (khoa) {
            // Serialize inventory changes on the SKU and share the parent locks
            // with other carts. Admin status writes take an exclusive parent lock.
            var maDaKhoa = jdbc.sql("SELECT ma_bien_the FROM bien_the_san_pham WHERE ma_bien_the = ? FOR UPDATE")
                .param(maBienThe).query(Long.class).optional();
            if (maDaKhoa.isEmpty()) return Optional.empty();
        }
        return jdbc.sql("SELECT " + COT_SKU + ", 0 AS so_luong " + NGUON_SKU + " WHERE b.ma_bien_the = ?"
                + (khoa ? " LOCK IN SHARE MODE" : "")).param(maBienThe)
            .query(GioHang_Repository::docMucGio).optional();
    }

    public List<MucGio> docBienTheGioKhach(Set<Long> maBienThe) {
        if (maBienThe.isEmpty()) return List.of();
        return jdbc.sql("SELECT " + COT_SKU + ", 0 AS so_luong " + NGUON_SKU
                + " WHERE b.ma_bien_the IN (:maBienThe) ORDER BY b.ma_bien_the")
            .param("maBienThe", maBienThe).query(GioHang_Repository::docMucGio).list();
    }

    public List<MucGio> docGio(long maNguoiDung) {
        return jdbc.sql("SELECT " + COT_SKU + ", ct.so_luong " + NGUON_SKU + """
            JOIN chi_tiet_gio_hang ct ON ct.ma_bien_the = b.ma_bien_the
            JOIN gio_hang g ON g.ma_gio_hang = ct.ma_gio_hang
            WHERE g.ma_nguoi_dung = ? ORDER BY b.ma_bien_the
            """)
            .param(maNguoiDung).query(GioHang_Repository::docMucGio).list();
    }

    private static MucGio docMucGio(ResultSet rs, int row) throws SQLException {
        return new MucGio(rs.getLong("ma_bien_the"), rs.getString("ten_san_pham"), rs.getString("ten_bien_the"),
            rs.getBigDecimal("gia_ban"), rs.getInt("kha_dung"), rs.getInt("so_luong"), rs.getString("sku"),
            rs.getString("url_hinh_anh"), rs.getString("ten_thuong_hieu"), rs.getString("duong_dan"),
            rs.getLong("ma_san_pham"), rs.getLong("ma_danh_muc"), rs.getLong("ma_thuong_hieu"));
    }

    public boolean ghiNhanGop(String maGop, long maNguoiDung) {
        try {
            jdbc.sql("INSERT INTO ghi_nhan_gop_gio_hang (ma_gop, ma_nguoi_dung) VALUES (?, ?)")
                .params(maGop, maNguoiDung).update();
            return true;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    public long khoaGio(long maNguoiDung) {
        jdbc.sql("""
            INSERT INTO gio_hang (ma_nguoi_dung) VALUES (?)
            ON DUPLICATE KEY UPDATE ma_nguoi_dung = VALUES(ma_nguoi_dung)
            """).param(maNguoiDung).update();
        return jdbc.sql("SELECT ma_gio_hang FROM gio_hang WHERE ma_nguoi_dung = ? FOR UPDATE")
            .param(maNguoiDung).query(Long.class).single();
    }

    public int soLuong(long maGio, long maBienThe) {
        return jdbc.sql("SELECT so_luong FROM chi_tiet_gio_hang WHERE ma_gio_hang = ? AND ma_bien_the = ?")
            .params(maGio, maBienThe).query(Integer.class).optional().orElse(0);
    }

    public void luuSoLuong(long maGio, long maBienThe, int soLuong) {
        if (soLuong == 0) {
            jdbc.sql("DELETE FROM chi_tiet_gio_hang WHERE ma_gio_hang = ? AND ma_bien_the = ?")
                .params(maGio, maBienThe).update();
        } else {
            jdbc.sql("""
                INSERT INTO chi_tiet_gio_hang (ma_gio_hang, ma_bien_the, so_luong) VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE so_luong = VALUES(so_luong)
                """).params(maGio, maBienThe, soLuong).update();
        }
        jdbc.sql("UPDATE gio_hang SET ngay_cap_nhat = UTC_TIMESTAMP(6) WHERE ma_gio_hang = ?").param(maGio).update();
    }

    public record MucGio(long maBienThe, String tenSanPham, String tenBienThe, BigDecimal giaBan, int khaDung,
                         int soLuong, String sku, String urlHinhAnh, String tenThuongHieu, String duongDan, long sanPham, long danhMuc, long thuongHieu) {
        public MucGio(long maBienThe, String tenSanPham, String tenBienThe, BigDecimal giaBan, int khaDung,
                      int soLuong, String sku, String urlHinhAnh, String tenThuongHieu, String duongDan) {
            this(maBienThe, tenSanPham, tenBienThe, giaBan, khaDung, soLuong, sku, urlHinhAnh, tenThuongHieu, duongDan, 0, 0, 0);
        }
        public MucGio {
            urlHinhAnh = AnhBienTheHienThi.hopLe(urlHinhAnh);
        }

        public BigDecimal thanhTien() { return giaBan.multiply(BigDecimal.valueOf(soLuong)); }

        public MucGio voiSoLuong(int soLuongMoi) {
            return new MucGio(maBienThe, tenSanPham, tenBienThe, giaBan, khaDung, soLuongMoi, sku,
                urlHinhAnh, tenThuongHieu, duongDan, sanPham, danhMuc, thuongHieu);
        }
    }

    public void xoaCacMuc(long maGio) {
        jdbc.sql("DELETE FROM chi_tiet_gio_hang WHERE ma_gio_hang = ?").param(maGio).update();
        jdbc.sql("UPDATE gio_hang SET ngay_cap_nhat = UTC_TIMESTAMP(6) WHERE ma_gio_hang = ?").param(maGio).update();
    }
}
