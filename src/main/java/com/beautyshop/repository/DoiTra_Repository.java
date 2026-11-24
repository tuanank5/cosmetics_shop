package com.beautyshop.repository;

import com.beautyshop.order.ReturnStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DoiTra_Repository {
    private final JdbcClient jdbc;
    DoiTra_Repository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public Optional<Don> timDon(long maDon, long maNguoiDung, boolean khoa) {
        return jdbc.sql("""
            SELECT ma_don_hang_hien_thi, trang_thai = 'DELIVERED' AND ngay_giao_thanh_cong IS NOT NULL
                AND UTC_TIMESTAMP(6) BETWEEN ngay_giao_thanh_cong AND TIMESTAMPADD(DAY, 7, ngay_giao_thanh_cong) AS con_han
            FROM don_hang WHERE ma_don_hang = ? AND ma_nguoi_dung = ?
            """ + (khoa ? " FOR UPDATE" : "")).params(maDon, maNguoiDung)
            .query((rs, row) -> new Don(maDon, rs.getString(1), rs.getBoolean(2))).optional();
    }

    public List<Muc> cacMuc(long maDon) {
        return jdbc.sql("""
            SELECT ct.ma_chi_tiet_don_hang, ct.ten_san_pham_khi_dat, ct.ten_bien_the_khi_dat,
                   ct.so_luong, GREATEST(0, ct.so_luong - COALESCE((
                       SELECT SUM(dt.so_luong) FROM chi_tiet_doi_tra dt
                       JOIN yeu_cau_doi_tra yc ON yc.ma_yeu_cau_doi_tra = dt.ma_yeu_cau_doi_tra
                       WHERE dt.ma_chi_tiet_don_hang = ct.ma_chi_tiet_don_hang AND yc.trang_thai <> 'REJECTED'
                   ), 0)) AS con_lai
            FROM chi_tiet_don_hang ct WHERE ct.ma_don_hang = ? ORDER BY ct.ma_chi_tiet_don_hang
            """).param(maDon).query((rs, row) -> new Muc(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getInt(4), rs.getInt(5))).list();
    }

    public long tao(long maDon, long maNguoiDung, String lyDo) {
        return jdbc.sql("""
            INSERT INTO yeu_cau_doi_tra (ma_don_hang, ma_nguoi_dung, trang_thai, ly_do, ngay_yeu_cau)
            VALUES (?, ?, 'PENDING', ?, UTC_TIMESTAMP(6)) RETURNING ma_yeu_cau_doi_tra
            """).params(maDon, maNguoiDung, lyDo).query(Long.class).single();
    }

    public void themMuc(long maYeuCau, long maMuc, int soLuong) {
        jdbc.sql("""
            INSERT INTO chi_tiet_doi_tra (ma_yeu_cau_doi_tra, ma_chi_tiet_don_hang, so_luong)
            VALUES (?, ?, ?)
            """).params(maYeuCau, maMuc, soLuong).update();
    }

    public List<YeuCau> danhSach(Long maNguoiDung) {
        var query = jdbc.sql("""
            SELECT yc.ma_yeu_cau_doi_tra, yc.ma_don_hang, d.ma_don_hang_hien_thi, yc.trang_thai
            FROM yeu_cau_doi_tra yc JOIN don_hang d ON d.ma_don_hang = yc.ma_don_hang
            """ + (maNguoiDung == null ? "" : " WHERE yc.ma_nguoi_dung = ? ") + " ORDER BY yc.ma_yeu_cau_doi_tra DESC");
        if (maNguoiDung != null) {
            query.param(maNguoiDung);
        }
        return query.query((rs, row) -> new YeuCau(rs.getLong(1), rs.getLong(2), rs.getString(3),
            ReturnStatus.valueOf(rs.getString(4)))).list();
    }

    public Optional<ChiTiet> chiTiet(long maYeuCau, Long maNguoiDung) {
        var query = jdbc.sql("""
            SELECT yc.ma_yeu_cau_doi_tra, yc.ma_don_hang, d.ma_don_hang_hien_thi, yc.trang_thai, yc.ly_do
            FROM yeu_cau_doi_tra yc JOIN don_hang d ON d.ma_don_hang = yc.ma_don_hang
            WHERE yc.ma_yeu_cau_doi_tra = ?
            """ + (maNguoiDung == null ? "" : " AND yc.ma_nguoi_dung = ?")).param(maYeuCau);
        if (maNguoiDung != null) {
            query.param(maNguoiDung);
        }
        return query.query((rs, row) -> new ChiTiet(new YeuCau(rs.getLong(1), rs.getLong(2), rs.getString(3),
            ReturnStatus.valueOf(rs.getString(4))), rs.getString(5))).optional();
    }

    public List<MucTra> mucDaYeuCau(long maYeuCau) {
        return jdbc.sql("""
            SELECT ct.ten_san_pham_khi_dat, ct.ten_bien_the_khi_dat, dt.so_luong
            FROM chi_tiet_doi_tra dt JOIN chi_tiet_don_hang ct ON ct.ma_chi_tiet_don_hang = dt.ma_chi_tiet_don_hang
            WHERE dt.ma_yeu_cau_doi_tra = ? ORDER BY ct.ma_chi_tiet_don_hang
            """).param(maYeuCau).query((rs, row) -> new MucTra(rs.getString(1), rs.getString(2), rs.getInt(3))).list();
    }

    public boolean chuyen(long maYeuCau, ReturnStatus hienTai, ReturnStatus dich) {
        return jdbc.sql("""
            UPDATE yeu_cau_doi_tra SET trang_thai = ? WHERE ma_yeu_cau_doi_tra = ? AND trang_thai = ?
            """).params(dich.name(), maYeuCau, hienTai.name()).update() == 1;
    }

    public record Don(long maDon, String maHienThi, boolean conHan) {}
    public record Muc(long maMuc, String tenSanPham, String tenBienThe, int daMua, int conLai) {}
    public record YeuCau(long ma, long maDon, String maHienThi, ReturnStatus trangThai) {}
    public record ChiTiet(YeuCau yeuCau, String lyDo) {}
    public record MucTra(String tenSanPham, String tenBienThe, int soLuong) {}
}
