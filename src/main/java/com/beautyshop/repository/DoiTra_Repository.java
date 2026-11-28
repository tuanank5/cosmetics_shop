package com.beautyshop.repository;

import com.beautyshop.order.ReturnStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
            INSERT INTO yeu_cau_doi_tra (ma_don_hang, ma_nguoi_dung, trang_thai, ly_do, ngay_yeu_cau, phien_ban_kiem)
            VALUES (?, ?, 'PENDING', ?, UTC_TIMESTAMP(6), 0) RETURNING ma_yeu_cau_doi_tra
            """).params(maDon, maNguoiDung, lyDo).query(Long.class).single();
    }

    public void themMuc(long maYeuCau, long maMuc, int soLuong) {
        jdbc.sql("""
            INSERT INTO chi_tiet_doi_tra (ma_yeu_cau_doi_tra, ma_chi_tiet_don_hang, so_luong)
            VALUES (?, ?, ?)
            """).params(maYeuCau, maMuc, soLuong).update();
    }

    public List<YeuCau> danhSach(Long maNguoiDung) {
        return danhSach(maNguoiDung, null);
    }

    public List<YeuCau> danhSach(Long maNguoiDung, Long maDon) {
        return docYeuCau(maNguoiDung, maDon, Pageable.unpaged());
    }

    public Page<YeuCau> docTrang(Long maNguoiDung, Pageable pageable) {
        var dem = jdbc.sql("SELECT COUNT(*) FROM yeu_cau_doi_tra"
            + (maNguoiDung == null ? "" : " WHERE ma_nguoi_dung = :nguoiDung"));
        if (maNguoiDung != null) dem.param("nguoiDung", maNguoiDung);
        return new PageImpl<>(docYeuCau(maNguoiDung, null, pageable), pageable, dem.query(Long.class).single());
    }

    private List<YeuCau> docYeuCau(Long maNguoiDung, Long maDon, Pageable pageable) {
        var query = jdbc.sql("""
            SELECT yc.ma_yeu_cau_doi_tra, yc.ma_don_hang, d.ma_don_hang_hien_thi, yc.trang_thai
            FROM yeu_cau_doi_tra yc JOIN don_hang d ON d.ma_don_hang = yc.ma_don_hang
            WHERE 1 = 1
            """ + (maNguoiDung == null ? "" : " AND yc.ma_nguoi_dung = :nguoiDung")
            + (maDon == null ? "" : " AND yc.ma_don_hang = :don")
            + " ORDER BY yc.ma_yeu_cau_doi_tra DESC"
            + (pageable.isPaged() ? " LIMIT :size OFFSET :offset" : ""));
        if (maNguoiDung != null) {
            query.param("nguoiDung", maNguoiDung);
        }
        if (maDon != null) {
            query.param("don", maDon);
        }
        if (pageable.isPaged()) query.param("size", pageable.getPageSize()).param("offset", pageable.getOffset());
        return query.query((rs, row) -> new YeuCau(rs.getLong(1), rs.getLong(2), rs.getString(3),
            ReturnStatus.valueOf(rs.getString(4)))).list();
    }

    public Optional<ChiTiet> chiTiet(long maYeuCau, Long maNguoiDung) {
        var query = jdbc.sql("""
            SELECT yc.ma_yeu_cau_doi_tra, yc.ma_don_hang, d.ma_don_hang_hien_thi, yc.trang_thai, yc.ly_do, yc.phien_ban_kiem
            FROM yeu_cau_doi_tra yc JOIN don_hang d ON d.ma_don_hang = yc.ma_don_hang
            WHERE yc.ma_yeu_cau_doi_tra = ?
            """ + (maNguoiDung == null ? "" : " AND yc.ma_nguoi_dung = ?")).param(maYeuCau);
        if (maNguoiDung != null) {
            query.param(maNguoiDung);
        }
        return query.query((rs, row) -> new ChiTiet(new YeuCau(rs.getLong(1), rs.getLong(2), rs.getString(3),
            ReturnStatus.valueOf(rs.getString(4))), rs.getString(5), rs.getObject(6, Long.class))).optional();
    }

    public List<MucTra> mucDaYeuCau(long maYeuCau) {
        return jdbc.sql("""
            SELECT dt.ma_chi_tiet_doi_tra AS ma, ct.ten_san_pham_khi_dat AS tenSanPham,
                   ct.ten_bien_the_khi_dat AS tenBienThe, dt.so_luong AS soLuong,
                   COALESCE(SUM(n.thuc_nhan),0) AS thucNhan,
                   COALESCE(SUM(IF(k.ma_xac_nhan IS NULL,0,q.du_ban)),0) AS duBan,
                   COALESCE(SUM(IF(k.ma_xac_nhan IS NULL,0,n.thuc_nhan-q.du_ban)),0) AS khongDu
            FROM chi_tiet_doi_tra dt JOIN chi_tiet_don_hang ct ON ct.ma_chi_tiet_don_hang = dt.ma_chi_tiet_don_hang
            LEFT JOIN dong_nhan_doi_tra n ON n.ma_dong_tra=dt.ma_chi_tiet_doi_tra
            LEFT JOIN ket_qua_kiem_doi_tra q ON q.ma_kiem=n.ma_kiem
            LEFT JOIN kien_doi_tra k ON k.ma_kien=n.ma_kien
            WHERE dt.ma_yeu_cau_doi_tra = ?
            GROUP BY dt.ma_chi_tiet_doi_tra,ct.ma_chi_tiet_don_hang,ct.ten_san_pham_khi_dat,ct.ten_bien_the_khi_dat,dt.so_luong
            ORDER BY ct.ma_chi_tiet_don_hang
            """).param(maYeuCau).query(MucTra.class).list();
    }

    public record DotNhan(java.time.LocalDateTime lucNhan, String bienThe, int soLuong,
                          Integer duBan, Integer daMo, Integer hong, Integer cachLyKhac) {}

    public List<DotNhan> dotNhanCuaKhach(long maYeuCau, long maNguoiDung) {
        return jdbc.sql("""
            SELECT t.luc_ghi AS lucNhan, c.ten_bien_the_khi_dat AS bienThe, n.thuc_nhan AS soLuong,
                   q.du_ban AS duBan, q.da_mo AS daMo, q.hong, q.khac AS cachLyKhac
            FROM yeu_cau_doi_tra y JOIN chi_tiet_doi_tra d USING(ma_yeu_cau_doi_tra)
            JOIN chi_tiet_don_hang c USING(ma_chi_tiet_don_hang)
            JOIN dong_nhan_doi_tra n ON n.ma_dong_tra=d.ma_chi_tiet_doi_tra
            JOIN kien_doi_tra k USING(ma_kien)
            JOIN thao_tac_doi_tra t ON t.ma_thao_tac=k.ma_nhan
            LEFT JOIN ket_qua_kiem_doi_tra q ON q.ma_kiem=n.ma_kiem AND k.ma_xac_nhan IS NOT NULL
            WHERE y.ma_yeu_cau_doi_tra=? AND y.ma_nguoi_dung=? ORDER BY k.ma_kien,n.ma_dong_nhan
            """).params(maYeuCau,maNguoiDung).query(DotNhan.class).list();
    }

    public boolean chuyen(long maYeuCau, ReturnStatus hienTai, ReturnStatus dich) {
        return jdbc.sql("""
            UPDATE yeu_cau_doi_tra SET trang_thai = ? WHERE ma_yeu_cau_doi_tra = ? AND trang_thai = ?
            """).params(dich.name(), maYeuCau, hienTai.name()).update() == 1;
    }

    public record Don(long maDon, String maHienThi, boolean conHan) {}
    public record Muc(long maMuc, String tenSanPham, String tenBienThe, int daMua, int conLai) {}
    public record YeuCau(long ma, long maDon, String maHienThi, ReturnStatus trangThai) {}
    public record ChiTiet(YeuCau yeuCau, String lyDo, Long phienBan) {}
    public record MucTra(long ma, String tenSanPham, String tenBienThe, int soLuong, int thucNhan, int duBan, int khongDu) {
        public int chuaNhan() { return soLuong - thucNhan; }
        public int choKiem() { return thucNhan - duBan - khongDu; }
    }
}
