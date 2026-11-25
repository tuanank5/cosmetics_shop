package com.beautyshop.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * P23: store dự phòng ({@code app.reviews.store=mariadb}) bọc bảng {@code danh_gia} cũ, giữ nguyên SQL và cơ chế
 * phiên bản SHA2 của nội dung. Bảng không có cột ngày duyệt nên bộ lọc "Chờ duyệt" và "Đã ẩn" đều là {@code hien_thi = FALSE}.
 */
@Repository
@ConditionalOnProperty(name = "app.reviews.store", havingValue = "mariadb")
public class MariaDbDanhGia_Store implements DanhGia_Store {
    private static final String PHIEN_BAN = "SHA2(CONCAT(dg.so_sao, ':', COALESCE(dg.noi_dung, '')), 256)";
    private static final String COT_ADMIN = "dg.ma_danh_gia, dg.ma_san_pham, ct.ma_don_hang, n.ho_ten, dg.so_sao, dg.noi_dung, "
        + "dg.hien_thi, " + PHIEN_BAN + ", dg.ngay_tao";
    private static final String TU_ADMIN = """
         FROM danh_gia dg JOIN chi_tiet_don_hang ct ON ct.ma_chi_tiet_don_hang = dg.ma_chi_tiet_don_hang
         JOIN nguoi_dung n ON n.ma_nguoi_dung = dg.ma_nguoi_dung
        """;
    private static final String PHAN_BO = "COUNT(*), COALESCE(SUM(so_sao), 0), "
        + "SUM(so_sao = 1), SUM(so_sao = 2), SUM(so_sao = 3), SUM(so_sao = 4), SUM(so_sao = 5)";

    private final JdbcClient jdbc;

    public MariaDbDanhGia_Store(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<DanhGiaCuaKhach> cuaChuMuc(long maChiTietDonHang, long maNguoiDung) {
        return jdbc.sql("""
            SELECT dg.ma_danh_gia, dg.so_sao, dg.noi_dung, dg.hien_thi
            FROM danh_gia dg WHERE dg.ma_chi_tiet_don_hang = ? AND dg.ma_nguoi_dung = ?
            """).params(maChiTietDonHang, maNguoiDung).query((rs, row) ->
                new DanhGiaCuaKhach(String.valueOf(rs.getLong(1)), rs.getInt(2), rs.getString(3), rs.getBoolean(4))).optional();
    }

    @Override
    public void tao(DanhGiaMoi moi) {
        jdbc.sql("""
            INSERT INTO danh_gia (ma_chi_tiet_don_hang, ma_nguoi_dung, ma_san_pham, so_sao, noi_dung, hien_thi, ngay_tao)
            VALUES (:muc, :nguoiDung, :sanPham, :soSao, :noiDung, FALSE, UTC_TIMESTAMP(6))
            """).param("muc", moi.maChiTietDonHang()).param("nguoiDung", moi.maNguoiDung()).param("sanPham", moi.maSanPham())
            .param("soSao", moi.soSao()).param("noiDung", moi.noiDung(), java.sql.Types.VARCHAR).update();
    }

    @Override
    public boolean sua(long maChiTietDonHang, long maNguoiDung, int soSao, String noiDung) {
        return jdbc.sql("""
            UPDATE danh_gia SET so_sao = :soSao, noi_dung = :noiDung, hien_thi = FALSE
            WHERE ma_chi_tiet_don_hang = :muc AND ma_nguoi_dung = :nguoiDung
            """).param("muc", maChiTietDonHang).param("nguoiDung", maNguoiDung)
            .param("soSao", soSao).param("noiDung", noiDung, java.sql.Types.VARCHAR).update() == 1;
    }

    @Override
    public Page<DanhGiaCongKhai> congKhai(long maSanPham, Pageable pageable) {
        List<DanhGiaCongKhai> trang = jdbc.sql("""
            SELECT n.ho_ten, dg.so_sao, dg.noi_dung, dg.ngay_tao FROM danh_gia dg
            JOIN nguoi_dung n ON n.ma_nguoi_dung = dg.ma_nguoi_dung
            WHERE dg.ma_san_pham = :sp AND dg.hien_thi = TRUE ORDER BY dg.ngay_tao DESC, dg.ma_danh_gia DESC
            LIMIT :n OFFSET :o
            """).param("sp", maSanPham).param("n", pageable.getPageSize()).param("o", pageable.getOffset())
            .query((rs, row) -> new DanhGiaCongKhai(DanhGia_Store.tenRutGon(rs.getString(1)), rs.getInt(2), rs.getString(3),
                utc(rs.getObject(4, LocalDateTime.class)))).list();
        long tong = jdbc.sql("SELECT COUNT(*) FROM danh_gia WHERE ma_san_pham = ? AND hien_thi = TRUE").param(maSanPham)
            .query(Long.class).single();
        return new PageImpl<>(trang, pageable, tong);
    }

    @Override
    public Page<DanhGiaAdmin> danhSachAdmin(BoLocDanhGia boLoc, Pageable pageable) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        Map<String, Object> thamSo = new HashMap<>();
        switch (boLoc.trangThai()) {
            case DANG_HIEN -> where.append(" AND dg.hien_thi = TRUE");
            case CHO_DUYET, DA_AN -> where.append(" AND dg.hien_thi = FALSE");
            case TAT_CA -> { }
        }
        if (boLoc.soSao() != null) {
            where.append(" AND dg.so_sao = :soSao");
            thamSo.put("soSao", boLoc.soSao());
        }
        if (boLoc.maSanPham() != null) {
            where.append(" AND dg.ma_san_pham = :sp");
            thamSo.put("sp", boLoc.maSanPham());
        }
        List<DanhGiaAdmin> trang = jdbc.sql("SELECT " + COT_ADMIN + TU_ADMIN + where
                + " ORDER BY dg.ngay_tao DESC, dg.ma_danh_gia DESC LIMIT :n OFFSET :o")
            .params(thamSo).param("n", pageable.getPageSize()).param("o", pageable.getOffset())
            .query((rs, row) -> admin(rs)).list();
        long tong = jdbc.sql("SELECT COUNT(*)" + TU_ADMIN + where).params(thamSo).query(Long.class).single();
        return new PageImpl<>(trang, pageable, tong);
    }

    @Override
    public boolean datHienThi(String ma, boolean hienThi, String phienBan) {
        long maDanhGia;
        try {
            maDanhGia = Long.parseLong(ma);
        } catch (NumberFormatException | NullPointerException exception) {
            return false;
        }
        if (!hienThi) {
            return jdbc.sql("UPDATE danh_gia SET hien_thi = FALSE WHERE ma_danh_gia = ?").param(maDanhGia).update() == 1;
        }
        return jdbc.sql("UPDATE danh_gia dg SET hien_thi = TRUE WHERE dg.ma_danh_gia = ? AND " + PHIEN_BAN + " = ?")
            .params(maDanhGia, phienBan).update() == 1;
    }

    @Override
    public Map<Long, TomTatDanhGia> tomTatNhieu(Collection<Long> maSanPham, Instant tuHoacNull) {
        Map<Long, TomTatDanhGia> ketQua = new HashMap<>();
        if (maSanPham.isEmpty()) {
            return ketQua;
        }
        var sql = jdbc.sql("SELECT ma_san_pham, " + PHAN_BO + " FROM danh_gia WHERE hien_thi = TRUE AND ma_san_pham IN (:ma)"
                + (tuHoacNull == null ? "" : " AND ngay_tao >= :tu") + " GROUP BY ma_san_pham").param("ma", maSanPham);
        if (tuHoacNull != null) {
            sql = sql.param("tu", LocalDateTime.ofInstant(tuHoacNull, ZoneOffset.UTC));
        }
        sql.query((rs, row) -> ketQua.put(rs.getLong(1), tomTat(rs, 2))).list();
        return ketQua;
    }

    @Override
    public TomTatDanhGia tomTatCuaHang(Instant tu) {
        return jdbc.sql("SELECT " + PHAN_BO + " FROM danh_gia WHERE hien_thi = TRUE AND ngay_tao >= ?")
            .param(LocalDateTime.ofInstant(tu, ZoneOffset.UTC)).query((rs, row) -> tomTat(rs, 1)).single();
    }

    @Override
    public ThongKeDanhGia thongKe(Instant a, Instant b) {
        long[] phanBo = new long[5];
        jdbc.sql("SELECT so_sao, COUNT(*) FROM danh_gia WHERE ngay_tao >= ? AND ngay_tao < ? GROUP BY so_sao")
            .params(LocalDateTime.ofInstant(a, ZoneOffset.UTC), LocalDateTime.ofInstant(b, ZoneOffset.UTC))
            .query((rs, row) -> {
                int sao = rs.getInt(1);
                if (sao >= 1 && sao <= 5) {
                    phanBo[sao - 1] = rs.getLong(2);
                }
                return sao;
            }).list();
        long choDuyet = jdbc.sql("SELECT COUNT(*) FROM danh_gia WHERE hien_thi = FALSE").query(Long.class).single();
        return new ThongKeDanhGia(choDuyet, phanBo);
    }

    private static TomTatDanhGia tomTat(ResultSet rs, int cot) throws SQLException {
        long[] phanBo = new long[5];
        for (int sao = 0; sao < 5; sao++) {
            phanBo[sao] = rs.getLong(cot + 2 + sao);
        }
        return new TomTatDanhGia(rs.getLong(cot), rs.getLong(cot + 1), phanBo);
    }

    private static DanhGiaAdmin admin(ResultSet rs) throws SQLException {
        return new DanhGiaAdmin(String.valueOf(rs.getLong(1)), rs.getLong(2), null, rs.getLong(3),
            DanhGia_Store.tenRutGon(rs.getString(4)), rs.getInt(5), rs.getString(6), rs.getBoolean(7), rs.getBoolean(7),
            rs.getString(8), utc(rs.getObject(9, LocalDateTime.class)));
    }

    private static Instant utc(LocalDateTime gio) {
        return gio == null ? null : gio.toInstant(ZoneOffset.UTC);
    }
}
