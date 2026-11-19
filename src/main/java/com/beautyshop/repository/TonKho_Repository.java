package com.beautyshop.repository;

import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TonKho_Repository {
    private final JdbcClient jdbc;

    TonKho_Repository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<String> khoaDonHang(long maDonHang) {
        return jdbc.sql("SELECT phuong_thuc_thanh_toan FROM don_hang WHERE ma_don_hang = ? FOR UPDATE")
            .param(maDonHang).query(String.class).optional();
    }

    public Optional<TamGiu> timTamGiu(long maDonHang, long maBienThe) {
        return jdbc.sql("""
            SELECT so_luong, trang_thai FROM tam_giu_ton_kho
            WHERE ma_don_hang = ? AND ma_bien_the = ? FOR UPDATE
            """).params(maDonHang, maBienThe)
            .query((rs, row) -> new TamGiu(rs.getInt("so_luong"), TrangThaiTamGiu.valueOf(rs.getString("trang_thai"))))
            .optional();
    }

    public void taoTamGiu(long maDonHang, long maBienThe, int soLuong) {
        jdbc.sql("""
            INSERT INTO tam_giu_ton_kho (ma_don_hang, ma_bien_the, so_luong, trang_thai, het_han_luc)
            VALUES (?, ?, ?, 'HELD', TIMESTAMPADD(MINUTE, 15, UTC_TIMESTAMP(6)))
            """).params(maDonHang, maBienThe, soLuong).update();
    }

    public void doiTrangThai(long maDonHang, long maBienThe, TrangThaiTamGiu trangThai) {
        jdbc.sql("""
            UPDATE tam_giu_ton_kho SET trang_thai = ?
            WHERE ma_don_hang = ? AND ma_bien_the = ?
            """).params(trangThai.name(), maDonHang, maBienThe).update();
    }

    public Optional<Integer> timSoLuongDaTruCOD(long maDonHang, long maBienThe) {
        return jdbc.sql("""
            SELECT so_luong FROM ghi_nhan_tru_kho_cod
            WHERE ma_don_hang = ? AND ma_bien_the = ? FOR UPDATE
            """).params(maDonHang, maBienThe).query(Integer.class).optional();
    }

    public void ghiNhanTruCOD(long maDonHang, long maBienThe, int soLuong) {
        jdbc.sql("""
            INSERT INTO ghi_nhan_tru_kho_cod (ma_don_hang, ma_bien_the, so_luong) VALUES (?, ?, ?)
            """).params(maDonHang, maBienThe, soLuong).update();
    }

    public enum TrangThaiTamGiu { HELD, CONSUMED, RELEASED }

    public record TamGiu(int soLuong, TrangThaiTamGiu trangThai) {}
}
