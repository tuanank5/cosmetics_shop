package com.beautyshop.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DanhGia_Repository {
    private static final String PHIEN_BAN = "SHA2(CONCAT(dg.so_sao, ':', COALESCE(dg.noi_dung, '')), 256)";
    private final JdbcClient jdbc;

    DanhGia_Repository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public Optional<MucMua> mucMua(long maMuc, long maNguoiDung) {
        return jdbc.sql("""
            SELECT b.ma_san_pham, ct.ten_san_pham_khi_dat, ct.ten_bien_the_khi_dat, d.ma_don_hang, d.trang_thai
            FROM chi_tiet_don_hang ct JOIN don_hang d ON d.ma_don_hang = ct.ma_don_hang
            JOIN bien_the_san_pham b ON b.ma_bien_the = ct.ma_bien_the
            WHERE ct.ma_chi_tiet_don_hang = ? AND d.ma_nguoi_dung = ?
            """).params(maMuc, maNguoiDung).query((rs, row) -> new MucMua(rs.getLong(1), rs.getString(2),
                rs.getString(3), rs.getLong(4), rs.getString(5))).optional();
    }

    public Optional<DanhGia> cuaChuMuc(long maMuc, long maNguoiDung) {
        return jdbc.sql("""
            SELECT dg.ma_danh_gia, dg.so_sao, dg.noi_dung, dg.hien_thi
            FROM danh_gia dg WHERE dg.ma_chi_tiet_don_hang = ? AND dg.ma_nguoi_dung = ?
            """).params(maMuc, maNguoiDung).query((rs, row) ->
                new DanhGia(rs.getLong(1), rs.getInt(2), rs.getString(3), rs.getBoolean(4))).optional();
    }

    public void tao(long maMuc, long maNguoiDung, long maSanPham, int soSao, String noiDung) {
        jdbc.sql("""
            INSERT INTO danh_gia (ma_chi_tiet_don_hang, ma_nguoi_dung, ma_san_pham, so_sao, noi_dung, hien_thi, ngay_tao)
            VALUES (:muc, :nguoiDung, :sanPham, :soSao, :noiDung, FALSE, UTC_TIMESTAMP(6))
            """).param("muc", maMuc).param("nguoiDung", maNguoiDung).param("sanPham", maSanPham)
            .param("soSao", soSao).param("noiDung", noiDung, java.sql.Types.VARCHAR).update();
    }

    public void sua(long maDanhGia, long maNguoiDung, int soSao, String noiDung) {
        jdbc.sql("""
            UPDATE danh_gia SET so_sao = :soSao, noi_dung = :noiDung, hien_thi = FALSE
            WHERE ma_danh_gia = :ma AND ma_nguoi_dung = :nguoiDung
            """).param("ma", maDanhGia).param("nguoiDung", maNguoiDung)
            .param("soSao", soSao).param("noiDung", noiDung, java.sql.Types.VARCHAR).update();
    }

    public List<DanhGia> congKhai(long maSanPham) {
        return jdbc.sql("""
            SELECT ma_danh_gia, so_sao, noi_dung, hien_thi FROM danh_gia
            WHERE ma_san_pham = ? AND hien_thi = TRUE ORDER BY ma_danh_gia DESC
            """).param(maSanPham).query((rs, row) ->
                new DanhGia(rs.getLong(1), rs.getInt(2), rs.getString(3), rs.getBoolean(4))).list();
    }

    public List<DanhGiaAdmin> danhSachAdmin() {
        return jdbc.sql("""
            SELECT dg.ma_danh_gia, dg.so_sao, dg.noi_dung, dg.hien_thi, s.ten_san_pham, d.ma_don_hang,
            """ + PHIEN_BAN + """

            FROM danh_gia dg JOIN san_pham s ON s.ma_san_pham = dg.ma_san_pham
            JOIN chi_tiet_don_hang d ON d.ma_chi_tiet_don_hang = dg.ma_chi_tiet_don_hang
            ORDER BY dg.ma_danh_gia DESC
            """).query((rs, row) -> new DanhGiaAdmin(new DanhGia(rs.getLong(1), rs.getInt(2), rs.getString(3),
                rs.getBoolean(4)), rs.getString(5), rs.getLong(6), rs.getString(7))).list();
    }

    public boolean datHienThi(long ma, boolean hienThi, String phienBan) {
        if (!hienThi) {
            return jdbc.sql("UPDATE danh_gia SET hien_thi = FALSE WHERE ma_danh_gia = ?").param(ma).update() == 1;
        }
        return jdbc.sql("UPDATE danh_gia dg SET hien_thi = TRUE WHERE dg.ma_danh_gia = ? AND " + PHIEN_BAN + " = ?")
            .params(ma, phienBan).update() == 1;
    }

    public record MucMua(long maSanPham, String tenSanPham, String tenBienThe, long maDon, String trangThai) {}
    public record DanhGia(long ma, int soSao, String noiDung, boolean hienThi) {}
    public record DanhGiaAdmin(DanhGia danhGia, String tenSanPham, long maDon, String phienBan) {}
}
