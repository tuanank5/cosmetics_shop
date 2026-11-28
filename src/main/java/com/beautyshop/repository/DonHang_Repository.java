package com.beautyshop.repository;

import com.beautyshop.dto.BoLocDonHangAdmin;
import com.beautyshop.order.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
    private static final ZoneId GIO_VIET_NAM = ZoneId.of("Asia/Ho_Chi_Minh");
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

    public void ghiPhi(long maDon, BigDecimal phiChuan, BigDecimal giamShip) {
        jdbc.sql("UPDATE don_hang SET phi_chuan=?, giam_van_chuyen=? WHERE ma_don_hang=?")
            .params(phiChuan, giamShip, maDon).update();
    }

    public void themMuc(long maDon, ChiTiet muc, BigDecimal giamHang) {
        jdbc.sql("""
            INSERT INTO chi_tiet_don_hang (ma_don_hang, ma_bien_the, sku_khi_dat, ten_san_pham_khi_dat,
                ten_bien_the_khi_dat, don_gia, so_luong, giam_hang) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """).params(maDon, muc.maBienThe(), muc.sku(), muc.tenSanPham(), muc.tenBienThe(), muc.donGia(), muc.soLuong(), giamHang).update();
    }

    public List<ChiTiet> chiTiet(long maDon) {
        return jdbc.sql("""
            SELECT c.ma_bien_the, c.sku_khi_dat, c.ten_san_pham_khi_dat, c.ten_bien_the_khi_dat, c.don_gia,
                   c.so_luong, c.ma_chi_tiet_don_hang, b.url_hinh_anh
            FROM chi_tiet_don_hang c
            LEFT JOIN bien_the_san_pham b ON b.ma_bien_the = c.ma_bien_the
            WHERE c.ma_don_hang = ? ORDER BY c.ma_bien_the
            """).param(maDon).query((rs, row) -> new ChiTiet(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getBigDecimal(5), rs.getInt(6), rs.getLong(7),
                AnhBienTheHienThi.hopLe(rs.getString(8)))).list();
    }

    public List<TomTat> danhSach(long maNguoiDung) {
        return docTomTat(maNguoiDung, Pageable.unpaged());
    }

    public Page<TomTat> danhSach(long maNguoiDung, Pageable pageable) {
        long tong = jdbc.sql("SELECT COUNT(*) FROM don_hang WHERE ma_nguoi_dung = ?")
            .param(maNguoiDung).query(Long.class).single();
        return new PageImpl<>(docTomTat(maNguoiDung, pageable), pageable, tong);
    }

    private List<TomTat> docTomTat(long maNguoiDung, Pageable pageable) {
        var query = jdbc.sql("""
            SELECT ma_don_hang, ma_don_hang_hien_thi, trang_thai, tong_thanh_toan, ngay_tao
            FROM don_hang WHERE ma_nguoi_dung = ? ORDER BY ma_don_hang DESC
            """ + (pageable.isPaged() ? " LIMIT ? OFFSET ?" : "")).param(maNguoiDung);
        if (pageable.isPaged()) query.params(pageable.getPageSize(), pageable.getOffset());
        return query.query((rs, row) -> new TomTat(rs.getLong(1), rs.getString(2),
            OrderStatus.valueOf(rs.getString(3)), rs.getBigDecimal(4), rs.getObject(5, LocalDateTime.class))).list();
    }

    private static final String TRANG_THAI_HIEU_LUC = "CASE WHEN d.phuong_thuc_thanh_toan = 'MOMO' THEN ("
        + ThanhToanMomo_Repository.sqlTrangThaiHienThi("d.ma_don_hang") + ") ELSE ("
        + sqlTrangThaiCOD("CONCAT('COD-', d.ma_don_hang)") + ") END";

    public Page<DongDonAdmin> danhSachAdmin(BoLocDonHangAdmin loc, Pageable pageable) {
        var thamSo = new LinkedHashMap<String, Object>();
        var dieuKien = new StringBuilder(" WHERE 1 = 1");
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
        if (loc.tuNgay() != null) {
            dieuKien.append(" AND d.ngay_tao >= :tuLuc");
            thamSo.put("tuLuc", loc.tuNgay().atStartOfDay(GIO_VIET_NAM).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
        }
        if (loc.denNgay() != null) {
            dieuKien.append(" AND d.ngay_tao < :denLuc");
            thamSo.put("denLuc", loc.denNgay().plusDays(1).atStartOfDay(GIO_VIET_NAM).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
        }
        long tong = jdbc.sql("SELECT COUNT(*) FROM don_hang d" + dieuKien).params(thamSo).query(Long.class).single();
        if (pageable.getOffset() >= tong) return new PageImpl<>(List.of(), pageable, tong);

        String thuTu = switch (loc.sapXep()) {
            case newest -> "d.ngay_tao DESC, d.ma_don_hang DESC";
            case oldest -> "d.ngay_tao ASC, d.ma_don_hang ASC";
            case totalDesc -> "d.tong_thanh_toan DESC, d.ma_don_hang DESC";
            case totalAsc -> "d.tong_thanh_toan ASC, d.ma_don_hang DESC";
        };
        var danhSach = jdbc.sql("""
            SELECT d.ma_don_hang, d.ma_don_hang_hien_thi, d.trang_thai, d.tong_thanh_toan,
                   d.phuong_thuc_thanh_toan, d.ngay_tao, n.ho_ten,
                   COALESCE((SELECT SUM(c.so_luong) FROM chi_tiet_don_hang c WHERE c.ma_don_hang = d.ma_don_hang), 0),
            """ + TRANG_THAI_HIEU_LUC + """
             FROM don_hang d JOIN nguoi_dung n ON n.ma_nguoi_dung = d.ma_nguoi_dung
            """ + dieuKien + " ORDER BY " + thuTu + " LIMIT :size OFFSET :offset")
            .params(thamSo).param("size", pageable.getPageSize()).param("offset", pageable.getOffset())
            .query((rs, row) -> new DongDonAdmin(rs.getLong(1), rs.getString(2), OrderStatus.valueOf(rs.getString(3)),
                rs.getBigDecimal(4), rs.getString(5), rs.getString(9), rs.getString(7), rs.getLong(8),
                rs.getObject(6, LocalDateTime.class))).list();
        return new PageImpl<>(danhSach, pageable, tong);
    }

    public TongQuan tongQuan() {
        return jdbc.sql("""
            SELECT COUNT(*), COUNT(da_thu), COALESCE(SUM(da_thu), 0),
                   COUNT(CASE WHEN trang_thai = 'PENDING_CONFIRMATION' THEN 1 END),
                   COUNT(CASE WHEN trang_thai IN ('CONFIRMED', 'PACKING', 'SHIPPING') THEN 1 END),
                   COUNT(CASE WHEN trang_thai = 'DELIVERED' THEN 1 END)
            FROM (
                SELECT CASE WHEN EXISTS (
                    SELECT 1 FROM lan_thanh_toan tt
                    WHERE tt.ma_don_hang = dh.ma_don_hang
                      AND tt.trang_thai = 'PAID' AND tt.so_tien = dh.tong_thanh_toan
                ) THEN dh.tong_thanh_toan END AS da_thu, dh.trang_thai
                FROM don_hang dh
            ) thong_ke
            """).query((rs, row) -> new TongQuan(rs.getLong(1), rs.getLong(2), rs.getBigDecimal(3),
                rs.getLong(4), rs.getLong(5), rs.getLong(6))).single();
    }

    public Optional<Don> timDon(long maDon, boolean khoa) {
        return jdbc.sql("""
            SELECT ma_don_hang, ma_don_hang_hien_thi, ma_phieu_giam_gia, ten_nguoi_nhan, sdt_nguoi_nhan,
                   dia_chi_khi_dat, tam_tinh, so_tien_giam, phi_van_chuyen, tong_thanh_toan, trang_thai,
                   phuong_thuc_thanh_toan, ngay_tao, ngay_giao_thanh_cong,
                   (SELECT n.ho_ten FROM nguoi_dung n WHERE n.ma_nguoi_dung = don_hang.ma_nguoi_dung), phi_chuan, giam_van_chuyen
            FROM don_hang WHERE ma_don_hang = ?
            """ + (khoa ? " FOR UPDATE" : "")).param(maDon)
            .query((rs, row) -> new Don(rs.getLong(1), rs.getString(2), rs.getObject(3, Long.class),
                rs.getString(4), rs.getString(5), rs.getString(6), rs.getBigDecimal(7), rs.getBigDecimal(8),
                rs.getBigDecimal(9), rs.getBigDecimal(10), OrderStatus.valueOf(rs.getString(11)),
                rs.getString(12), rs.getObject(13, LocalDateTime.class), rs.getObject(14, LocalDateTime.class),
                rs.getString(15), rs.getBigDecimal(16), rs.getBigDecimal(17))).optional();
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
                          BigDecimal donGia, int soLuong, Long maMuc, String urlHinhAnh) {
        public ChiTiet(long maBienThe, String sku, String tenSanPham, String tenBienThe,
                       BigDecimal donGia, int soLuong, Long maMuc) {
            this(maBienThe, sku, tenSanPham, tenBienThe, donGia, soLuong, maMuc, null);
        }

        public BigDecimal thanhTien() { return donGia.multiply(BigDecimal.valueOf(soLuong)); }
    }
    public record TomTat(long maDonHang, String maHienThi, OrderStatus trangThai, BigDecimal tong, LocalDateTime ngayTao) {}
    public record DongDonAdmin(long maDonHang, String maHienThi, OrderStatus trangThai, BigDecimal tong,
                              String phuongThuc, String thanhToan, String tenKhachHang, long soSanPham, LocalDateTime ngayTao) {
        public String nhanThanhToan() { return BoLocDonHangAdmin.ThanhToan.nhan(thanhToan); }
        public ZonedDateTime gioDat() { return gioVietNam(ngayTao); }
    }
    public record TongQuan(long tongSoDon, long soDonDaThu, BigDecimal doanhThuDaThu,
                           long choXacNhan, long dangXuLy, long daGiao) {
        public TongQuan(long tongSoDon, long soDonDaThu, BigDecimal doanhThuDaThu) {
            this(tongSoDon, soDonDaThu, doanhThuDaThu, 0, 0, 0);
        }
    }
    public record Don(long maDonHang, String maHienThi, Long maVoucher, String tenNguoiNhan, String soDienThoai,
                      String diaChi, BigDecimal tamTinh, BigDecimal giam, BigDecimal phiVanChuyen,
                      BigDecimal tong, OrderStatus trangThai, String phuongThuc, LocalDateTime ngayTao,
                      LocalDateTime ngayGiaoThanhCong, String tenKhachHang, BigDecimal phiChuan, BigDecimal giamShip) {
        public Don(long maDonHang, String maHienThi, Long maVoucher, String tenNguoiNhan, String soDienThoai,
                   String diaChi, BigDecimal tamTinh, BigDecimal giam, BigDecimal phiVanChuyen, BigDecimal tong,
                   OrderStatus trangThai, String phuongThuc, LocalDateTime ngayTao) {
            this(maDonHang, maHienThi, maVoucher, tenNguoiNhan, soDienThoai, diaChi, tamTinh, giam, phiVanChuyen, tong,
                trangThai, phuongThuc, ngayTao, null, null, null, null);
        }

        public ZonedDateTime gioDat() { return gioVietNam(ngayTao); }
        public ZonedDateTime gioGiao() { return gioVietNam(ngayGiaoThanhCong); }
    }

    private static ZonedDateTime gioVietNam(LocalDateTime luc) {
        return luc == null ? null : luc.atOffset(ZoneOffset.UTC).atZoneSameInstant(GIO_VIET_NAM);
    }
}
