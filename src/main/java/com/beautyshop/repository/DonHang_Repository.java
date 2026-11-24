package com.beautyshop.repository;

import com.beautyshop.order.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DonHang_Repository {
    private final JdbcClient jdbc;

    DonHang_Repository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public long tao(String maHienThi, long maNguoiDung, Long maVoucher, String ten, String sdt, String diaChi,
                    BigDecimal tamTinh, BigDecimal giam, BigDecimal phi, BigDecimal tong, boolean online) {
        return jdbc.sql("""
            INSERT INTO don_hang (ma_don_hang_hien_thi, ma_nguoi_dung, ma_phieu_giam_gia,
                ten_nguoi_nhan, sdt_nguoi_nhan, dia_chi_khi_dat, tam_tinh, so_tien_giam, phi_van_chuyen,
                tong_thanh_toan, trang_thai, phuong_thuc_thanh_toan, ngay_tao)
            VALUES (:code, :user, :voucher, :ten, :sdt, :diaChi, :tamTinh, :giam, :phi, :tong,
                :trangThai, :phuongThuc, UTC_TIMESTAMP(6)) RETURNING ma_don_hang
            """).param("code", maHienThi).param("user", maNguoiDung).param("voucher", maVoucher, java.sql.Types.BIGINT)
            .param("ten", ten).param("sdt", sdt).param("diaChi", diaChi).param("tamTinh", tamTinh)
            .param("giam", giam).param("phi", phi).param("tong", tong)
            .param("trangThai", online ? OrderStatus.PENDING_PAYMENT.name() : OrderStatus.PENDING_CONFIRMATION.name())
            .param("phuongThuc", online ? "MOMO" : "COD").query(Long.class).single();
    }

    public void themMuc(long maDon, ChiTiet muc) {
        jdbc.sql("""
            INSERT INTO chi_tiet_don_hang (ma_don_hang, ma_bien_the, sku_khi_dat, ten_san_pham_khi_dat,
                ten_bien_the_khi_dat, don_gia, so_luong) VALUES (?, ?, ?, ?, ?, ?, ?)
            """).params(maDon, muc.maBienThe(), muc.sku(), muc.tenSanPham(), muc.tenBienThe(), muc.donGia(), muc.soLuong()).update();
    }

    public List<ChiTiet> chiTiet(long maDon) {
        return jdbc.sql("""
            SELECT ma_bien_the, sku_khi_dat, ten_san_pham_khi_dat, ten_bien_the_khi_dat, don_gia, so_luong, ma_chi_tiet_don_hang
            FROM chi_tiet_don_hang WHERE ma_don_hang = ? ORDER BY ma_bien_the
            """).param(maDon).query((rs, row) -> new ChiTiet(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getBigDecimal(5), rs.getInt(6), rs.getLong(7))).list();
    }

    public List<TomTat> danhSach(Long maNguoiDung) {
        var truyVan = jdbc.sql("""
            SELECT ma_don_hang, ma_don_hang_hien_thi, trang_thai, tong_thanh_toan, ngay_tao
            FROM don_hang
            """ + (maNguoiDung == null ? "" : " WHERE ma_nguoi_dung = ? ") + " ORDER BY ma_don_hang DESC");
        if (maNguoiDung != null) {
            truyVan.param(maNguoiDung);
        }
        return truyVan.query((rs, row) -> new TomTat(rs.getLong(1), rs.getString(2),
            OrderStatus.valueOf(rs.getString(3)), rs.getBigDecimal(4), rs.getObject(5, LocalDateTime.class))).list();
    }

    public TongQuan tongQuan() {
        return jdbc.sql("""
            SELECT COUNT(*), COUNT(da_thu), COALESCE(SUM(da_thu), 0)
            FROM (
                SELECT CASE WHEN EXISTS (
                    SELECT 1 FROM lan_thanh_toan tt
                    WHERE tt.ma_don_hang = dh.ma_don_hang
                      AND tt.trang_thai = 'PAID' AND tt.so_tien = dh.tong_thanh_toan
                ) THEN dh.tong_thanh_toan END AS da_thu
                FROM don_hang dh
            ) thong_ke
            """).query((rs, row) -> new TongQuan(rs.getLong(1), rs.getLong(2), rs.getBigDecimal(3))).single();
    }

    public Optional<Don> timDon(long maDon, boolean khoa) {
        return jdbc.sql("""
            SELECT ma_don_hang, ma_don_hang_hien_thi, ma_phieu_giam_gia, ten_nguoi_nhan, sdt_nguoi_nhan,
                   dia_chi_khi_dat, tam_tinh, so_tien_giam, phi_van_chuyen, tong_thanh_toan, trang_thai,
                   phuong_thuc_thanh_toan, ngay_tao
            FROM don_hang WHERE ma_don_hang = ?
            """ + (khoa ? " FOR UPDATE" : "")).param(maDon)
            .query((rs, row) -> new Don(rs.getLong(1), rs.getString(2), rs.getObject(3, Long.class),
                rs.getString(4), rs.getString(5), rs.getString(6), rs.getBigDecimal(7), rs.getBigDecimal(8),
                rs.getBigDecimal(9), rs.getBigDecimal(10), OrderStatus.valueOf(rs.getString(11)),
                rs.getString(12), rs.getObject(13, LocalDateTime.class))).optional();
    }

    public void doiTrangThai(long maDon, OrderStatus trangThai) {
        if (trangThai == OrderStatus.DELIVERED) {
            jdbc.sql("UPDATE don_hang SET trang_thai = ?, ngay_giao_thanh_cong = UTC_TIMESTAMP(6) WHERE ma_don_hang = ?")
                .params(trangThai.name(), maDon).update();
        } else {
            jdbc.sql("UPDATE don_hang SET trang_thai = ? WHERE ma_don_hang = ?")
                .params(trangThai.name(), maDon).update();
        }
    }

    public void guiHang(long maDon) {
        jdbc.sql("""
            INSERT INTO van_chuyen (ma_don_hang, trang_thai, ngay_gui_hang)
            VALUES (?, 'SHIPPING', UTC_TIMESTAMP(6))
            """).param(maDon).update();
    }

    public boolean giaoXong(long maDon) {
        return jdbc.sql("""
            UPDATE van_chuyen SET trang_thai = 'DELIVERED', ngay_giao_thanh_cong = UTC_TIMESTAMP(6)
            WHERE ma_don_hang = ? AND trang_thai = 'SHIPPING'
            """).param(maDon).update() == 1;
    }

    public void taoThanhToanCOD(long maDon, BigDecimal tong) {
        jdbc.sql("""
            INSERT INTO lan_thanh_toan (ma_don_hang, ma_yeu_cau, cong_thanh_toan, so_tien, trang_thai, ngay_tao)
            VALUES (?, ?, 'COD', ?, 'UNPAID', UTC_TIMESTAMP(6))
            """).params(maDon, "COD-" + maDon, tong).update();
    }

    public Optional<String> trangThaiThanhToanCOD(long maDon) {
        return jdbc.sql("SELECT trang_thai FROM lan_thanh_toan WHERE ma_yeu_cau = ? AND cong_thanh_toan = 'COD'")
            .param("COD-" + maDon).query(String.class).optional();
    }

    public boolean thuTienCOD(long maDon) {
        return jdbc.sql("""
            UPDATE lan_thanh_toan SET trang_thai = 'PAID', ngay_thanh_toan = UTC_TIMESTAMP(6)
            WHERE ma_yeu_cau = ? AND cong_thanh_toan = 'COD' AND trang_thai = 'UNPAID'
            """).param("COD-" + maDon).update() == 1;
    }

    public record ChiTiet(long maBienThe, String sku, String tenSanPham, String tenBienThe,
                          BigDecimal donGia, int soLuong, Long maMuc) {
        public BigDecimal thanhTien() { return donGia.multiply(BigDecimal.valueOf(soLuong)); }
    }
    public record TomTat(long maDonHang, String maHienThi, OrderStatus trangThai, BigDecimal tong, LocalDateTime ngayTao) {}
    public record TongQuan(long tongSoDon, long soDonDaThu, BigDecimal doanhThuDaThu) {}
    public record Don(long maDonHang, String maHienThi, Long maVoucher, String tenNguoiNhan, String soDienThoai,
                      String diaChi, BigDecimal tamTinh, BigDecimal giam, BigDecimal phiVanChuyen,
                      BigDecimal tong, OrderStatus trangThai, String phuongThuc, LocalDateTime ngayTao) {}
}
