package com.beautyshop.repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PhieuGiamGia_Repository {
    private static final String DOC_PHIEU = """
        SELECT ma_phieu_giam_gia, ma_ap_dung, loai_giam_gia, gia_tri_giam, muc_giam_toi_da, don_toi_thieu,
            gioi_han_su_dung, bat_dau_luc, ket_thuc_luc, dang_hoat_dong, so_luot_da_dung, so_luot_tam_giu
        FROM phieu_giam_gia
        """;
    private static final RowMapper<ThongTin> ANH_XA_PHIEU = (rs, row) -> new ThongTin(rs.getLong(1),
        new CauHinh(rs.getString(2), rs.getString(3), rs.getBigDecimal(4), rs.getBigDecimal(5),
            rs.getBigDecimal(6), rs.getInt(7), rs.getObject(8, LocalDateTime.class),
            rs.getObject(9, LocalDateTime.class), rs.getBoolean(10)), rs.getInt(11), rs.getInt(12));

    private final JdbcClient jdbc;

    PhieuGiamGia_Repository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public List<ThongTin> danhSach() {
        return jdbc.sql(DOC_PHIEU + " ORDER BY ma_phieu_giam_gia DESC").query(ANH_XA_PHIEU).list();
    }

    public Optional<ThongTin> timTheoMa(long ma, boolean khoa) {
        return jdbc.sql(DOC_PHIEU + " WHERE ma_phieu_giam_gia = ?" + (khoa ? " FOR UPDATE" : ""))
            .param(ma).query(ANH_XA_PHIEU).optional();
    }

    public void tao(CauHinh phieu) {
        ganCauHinh(jdbc.sql("""
            INSERT INTO phieu_giam_gia (ma_ap_dung, loai_giam_gia, gia_tri_giam, muc_giam_toi_da,
                don_toi_thieu, gioi_han_su_dung, bat_dau_luc, ket_thuc_luc, dang_hoat_dong)
            VALUES (:maApDung, :loai, :giaTri, :toiDa, :toiThieu, :gioiHan, :batDau, :ketThuc, :hoatDong)
            """), phieu).update();
    }

    public void sua(long ma, CauHinh phieu) {
        ganCauHinh(jdbc.sql("""
            UPDATE phieu_giam_gia
            SET ma_ap_dung = :maApDung, loai_giam_gia = :loai, gia_tri_giam = :giaTri,
                muc_giam_toi_da = :toiDa, don_toi_thieu = :toiThieu, gioi_han_su_dung = :gioiHan,
                bat_dau_luc = :batDau, ket_thuc_luc = :ketThuc, dang_hoat_dong = :hoatDong
            WHERE ma_phieu_giam_gia = :ma
            """).param("ma", ma), phieu).update();
    }

    public boolean xoaChuaSuDung(long ma) {
        return jdbc.sql("""
            DELETE FROM phieu_giam_gia
            WHERE ma_phieu_giam_gia = :ma AND so_luot_da_dung = 0 AND so_luot_tam_giu = 0
              AND NOT EXISTS (SELECT 1 FROM don_hang WHERE ma_phieu_giam_gia = :ma)
              AND NOT EXISTS (SELECT 1 FROM tam_giu_phieu_giam_gia WHERE ma_phieu_giam_gia = :ma)
            """).param("ma", ma).update() == 1;
    }

    private JdbcClient.StatementSpec ganCauHinh(JdbcClient.StatementSpec cauLenh, CauHinh phieu) {
        return cauLenh.param("maApDung", phieu.maApDung()).param("loai", phieu.loaiGiamGia())
            .param("giaTri", phieu.giaTriGiam()).param("toiDa", phieu.mucGiamToiDa(), Types.DECIMAL)
            .param("toiThieu", phieu.donToiThieu()).param("gioiHan", phieu.gioiHanSuDung())
            .param("batDau", phieu.batDauLuc()).param("ketThuc", phieu.ketThucLuc())
            .param("hoatDong", phieu.dangHoatDong());
    }

    public Optional<Phieu> timPhieuHopLe(String ma, boolean khoa) {
        return jdbc.sql("""
            SELECT ma_phieu_giam_gia, loai_giam_gia, gia_tri_giam, muc_giam_toi_da, don_toi_thieu
            FROM phieu_giam_gia
            WHERE ma_ap_dung = ? AND dang_hoat_dong = TRUE
              AND bat_dau_luc <= UTC_TIMESTAMP(6) AND ket_thuc_luc > UTC_TIMESTAMP(6)
              AND so_luot_da_dung + so_luot_tam_giu < gioi_han_su_dung
            """ + (khoa ? " FOR UPDATE" : "")).param(ma).query((rs, row) -> new Phieu(rs.getLong(1), rs.getString(2),
                rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getBigDecimal(5))).optional();
    }

    public void ghiNhanSuDung(long ma) {
        jdbc.sql("UPDATE phieu_giam_gia SET so_luot_da_dung = so_luot_da_dung + 1 WHERE ma_phieu_giam_gia = ?")
            .param(ma).update();
    }

    public boolean hoanLuot(long ma) {
        return jdbc.sql("""
            UPDATE phieu_giam_gia SET so_luot_da_dung = so_luot_da_dung - 1
            WHERE ma_phieu_giam_gia = ? AND so_luot_da_dung > 0
            """).param(ma).update() == 1;
    }

    public void tamGiu(long ma) {
        jdbc.sql("UPDATE phieu_giam_gia SET so_luot_tam_giu = so_luot_tam_giu + 1 WHERE ma_phieu_giam_gia = ?")
            .param(ma).update();
    }

    public void ghiTamGiu(long maDon, long maVoucher) {
        jdbc.sql("""
            INSERT INTO tam_giu_phieu_giam_gia (ma_phieu_giam_gia, ma_don_hang, trang_thai, ngay_tao)
            VALUES (?, ?, 'HELD', UTC_TIMESTAMP(6))
            """).params(maVoucher, maDon).update();
    }

    public boolean ketThucTamGiu(long maDon, long maVoucher, boolean suDung) {
        String trangThai = suDung ? "CONSUMED" : "RELEASED";
        int daChuyen = jdbc.sql("""
            UPDATE tam_giu_phieu_giam_gia SET trang_thai = ?
            WHERE ma_don_hang = ? AND ma_phieu_giam_gia = ? AND trang_thai = 'HELD'
            """).params(trangThai, maDon, maVoucher).update();
        if (daChuyen == 0) {
            return jdbc.sql("""
                SELECT trang_thai FROM tam_giu_phieu_giam_gia
                WHERE ma_don_hang = ? AND ma_phieu_giam_gia = ? FOR UPDATE
                """).params(maDon, maVoucher).query(String.class).optional().filter(trangThai::equals).isPresent();
        }
        return jdbc.sql("""
            UPDATE phieu_giam_gia SET so_luot_tam_giu = so_luot_tam_giu - 1,
                so_luot_da_dung = so_luot_da_dung + ?
            WHERE ma_phieu_giam_gia = ? AND so_luot_tam_giu > 0
            """).params(suDung ? 1 : 0, maVoucher).update() == 1;
    }

    public record Phieu(long ma, String loai, BigDecimal giaTri, BigDecimal toiDa, BigDecimal toiThieu) {}
    public record CauHinh(String maApDung, String loaiGiamGia, BigDecimal giaTriGiam, BigDecimal mucGiamToiDa,
                          BigDecimal donToiThieu, int gioiHanSuDung, LocalDateTime batDauLuc,
                          LocalDateTime ketThucLuc, boolean dangHoatDong) {}
    public record ThongTin(long maPhieuGiamGia, CauHinh cauHinh, int soLuotDaDung, int soLuotTamGiu) {
        public int soLuotConLai() { return cauHinh.gioiHanSuDung() - soLuotDaDung - soLuotTamGiu; }
    }
}
