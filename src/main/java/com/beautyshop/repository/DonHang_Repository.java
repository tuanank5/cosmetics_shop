package com.beautyshop.repository;

import com.beautyshop.dto.BoLocDonHangAdmin;
import com.beautyshop.order.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

    public List<TomTat> danhSach(long maNguoiDung) {
        return jdbc.sql("""
            SELECT ma_don_hang, ma_don_hang_hien_thi, trang_thai, tong_thanh_toan, ngay_tao
            FROM don_hang WHERE ma_nguoi_dung = ? ORDER BY ma_don_hang DESC
            """).param(maNguoiDung).query((rs, row) -> new TomTat(rs.getLong(1), rs.getString(2),
            OrderStatus.valueOf(rs.getString(3)), rs.getBigDecimal(4), rs.getObject(5, LocalDateTime.class))).list();
    }

    private static final String TRANG_THAI_HIEU_LUC = "CASE WHEN d.phuong_thuc_thanh_toan = 'MOMO' THEN ("
        + ThanhToanMomo_Repository.sqlTrangThaiHienThi("d.ma_don_hang") + ") ELSE ("
        + sqlTrangThaiCOD("CONCAT('COD-', d.ma_don_hang)") + ") END";

    public Page<DongDonAdmin> danhSachAdmin(BoLocDonHangAdmin loc, Pageable pageable) {
        var thamSo = new LinkedHashMap<String, Object>();
        var dieuKien = new StringBuilder(" FROM don_hang d WHERE 1 = 1");
        if (!loc.trangThai().isEmpty()) {
            dieuKien.append(" AND d.trang_thai IN (:trangThai)");
            thamSo.put("trangThai", loc.trangThai().stream().map(Enum::name).toList());
        }
        if (loc.phuongThuc() != null) {
            dieuKien.append(" AND d.phuong_thuc_thanh_toan = :phuongThuc");
            thamSo.put("phuongThuc", loc.phuongThuc().name());
        }
        if (!loc.maDon().isEmpty()) {
            dieuKien.append(" AND d.ma_don_hang_hien_thi LIKE :maDon ESCAPE '!'");
            thamSo.put("maDon", loc.maDon().replace("_", "!_") + "%");
        }
        if (loc.thanhToan() != null) {
            dieuKien.append(" AND (").append(TRANG_THAI_HIEU_LUC).append(") = :thanhToan");
            thamSo.put("thanhToan", loc.thanhToan().name());
        }
        long tong = jdbc.sql("SELECT COUNT(*)" + dieuKien).params(thamSo).query(Long.class).single();
        if (pageable.getOffset() >= tong) return new PageImpl<>(List.of(), pageable, tong);

        String thuTu = switch (loc.sapXep()) {
            case newest -> "d.ngay_tao DESC, d.ma_don_hang DESC";
            case oldest -> "d.ngay_tao ASC, d.ma_don_hang ASC";
            case totalDesc -> "d.tong_thanh_toan DESC, d.ma_don_hang DESC";
            case totalAsc -> "d.tong_thanh_toan ASC, d.ma_don_hang DESC";
        };
        var danhSach = jdbc.sql("""
            SELECT d.ma_don_hang, d.ma_don_hang_hien_thi, d.trang_thai, d.tong_thanh_toan,
                   d.phuong_thuc_thanh_toan,
            """ + TRANG_THAI_HIEU_LUC + dieuKien + " ORDER BY " + thuTu + " LIMIT :size OFFSET :offset")
            .params(thamSo).param("size", pageable.getPageSize()).param("offset", pageable.getOffset())
            .query((rs, row) -> new DongDonAdmin(rs.getLong(1), rs.getString(2), OrderStatus.valueOf(rs.getString(3)),
                rs.getBigDecimal(4), rs.getString(5), rs.getString(6))).list();
        return new PageImpl<>(danhSach, pageable, tong);
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
        return jdbc.sql(sqlTrangThaiCOD("?"))
            .param("COD-" + maDon).query(String.class).optional();
    }

    private static String sqlTrangThaiCOD(String maYeuCau) {
        return "SELECT trang_thai FROM lan_thanh_toan WHERE ma_yeu_cau = " + maYeuCau + " AND cong_thanh_toan = 'COD'";
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
    public record DongDonAdmin(long maDonHang, String maHienThi, OrderStatus trangThai, BigDecimal tong,
                              String phuongThuc, String thanhToan) {
        public String nhanThanhToan() { return BoLocDonHangAdmin.ThanhToan.nhan(thanhToan); }
    }
    public record TongQuan(long tongSoDon, long soDonDaThu, BigDecimal doanhThuDaThu) {}
    public record Don(long maDonHang, String maHienThi, Long maVoucher, String tenNguoiNhan, String soDienThoai,
                      String diaChi, BigDecimal tamTinh, BigDecimal giam, BigDecimal phiVanChuyen,
                      BigDecimal tong, OrderStatus trangThai, String phuongThuc, LocalDateTime ngayTao) {}
}
