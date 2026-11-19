package com.beautyshop.repository;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PhieuGiamGia_Repository {
    private final JdbcClient jdbc;

    PhieuGiamGia_Repository(JdbcClient jdbc) { this.jdbc = jdbc; }

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
        int daChuyen = jdbc.sql("""
            UPDATE tam_giu_phieu_giam_gia SET trang_thai = ?
            WHERE ma_don_hang = ? AND ma_phieu_giam_gia = ? AND trang_thai = 'HELD'
            """).params(suDung ? "CONSUMED" : "RELEASED", maDon, maVoucher).update();
        return daChuyen == 1 && jdbc.sql("""
            UPDATE phieu_giam_gia SET so_luot_tam_giu = so_luot_tam_giu - 1,
                so_luot_da_dung = so_luot_da_dung + ?
            WHERE ma_phieu_giam_gia = ? AND so_luot_tam_giu > 0
            """).params(suDung ? 1 : 0, maVoucher).update() == 1;
    }

    public record Phieu(long ma, String loai, BigDecimal giaTri, BigDecimal toiDa, BigDecimal toiThieu) {}
}
