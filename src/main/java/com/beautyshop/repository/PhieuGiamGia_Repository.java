package com.beautyshop.repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PhieuGiamGia_Repository {
    private static final String DOC_PHIEU = """
        SELECT ma_phieu_giam_gia, ma_ap_dung, loai_giam_gia, gia_tri_giam, muc_giam_toi_da, don_toi_thieu,
            gioi_han_su_dung, bat_dau_luc, ket_thuc_luc, dang_hoat_dong, so_luot_da_dung, so_luot_tam_giu, slot, gioi_han_khach
        FROM phieu_giam_gia
        """;
    private static final RowMapper<ThongTin> ANH_XA_PHIEU = (rs, row) -> new ThongTin(rs.getLong(1),
        new CauHinh(rs.getString(2), rs.getString(3), rs.getBigDecimal(4), rs.getBigDecimal(5),
            rs.getBigDecimal(6), rs.getInt(7), rs.getObject(8, LocalDateTime.class),
            rs.getObject(9, LocalDateTime.class), rs.getBoolean(10)), rs.getInt(11), rs.getInt(12), rs.getString(13), rs.getObject(14,Integer.class));

    private final JdbcClient jdbc;

    PhieuGiamGia_Repository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public List<ThongTin> danhSach() {
        return jdbc.sql(DOC_PHIEU + " ORDER BY ma_phieu_giam_gia DESC").query(ANH_XA_PHIEU).list();
    }

    public Page<ThongTin> docTrang(String mauTim, Boolean active, Pageable pageable) {
        String loc = """
            WHERE (:q = '' OR LOWER(ma_ap_dung) LIKE :q ESCAPE '!')
              AND (:active IS NULL OR dang_hoat_dong = :active)
            """;
        long tong = jdbc.sql("SELECT COUNT(*) FROM phieu_giam_gia " + loc)
            .param("q", mauTim).param("active", active, Types.BOOLEAN).query(Long.class).single();
        var muc = jdbc.sql(DOC_PHIEU + loc + " ORDER BY ma_phieu_giam_gia DESC LIMIT :size OFFSET :offset")
            .param("q", mauTim).param("active", active, Types.BOOLEAN)
            .param("size", pageable.getPageSize()).param("offset", pageable.getOffset()).query(ANH_XA_PHIEU).list();
        return new PageImpl<>(muc, pageable, tong);
    }

    public Optional<ThongTin> timTheoMa(long ma, boolean khoa) {
        return jdbc.sql(DOC_PHIEU + " WHERE ma_phieu_giam_gia = ?" + (khoa ? " FOR UPDATE" : ""))
            .param(ma).query(ANH_XA_PHIEU).optional();
    }

    public long tao(CauHinh phieu) {
        return ganCauHinh(jdbc.sql("""
            INSERT INTO phieu_giam_gia (ma_ap_dung, loai_giam_gia, gia_tri_giam, muc_giam_toi_da,
                don_toi_thieu, gioi_han_su_dung, bat_dau_luc, ket_thuc_luc, dang_hoat_dong)
            VALUES (:maApDung, :loai, :giaTri, :toiDa, :toiThieu, :gioiHan, :batDau, :ketThuc, :hoatDong)
            RETURNING ma_phieu_giam_gia
            """), phieu).query(Long.class).single();
    }

    public boolean sua(long ma, CauHinh phieu) {
        return ganCauHinh(jdbc.sql("""
            UPDATE phieu_giam_gia
            SET ma_ap_dung = :maApDung, loai_giam_gia = :loai, gia_tri_giam = :giaTri,
                muc_giam_toi_da = :toiDa, don_toi_thieu = :toiThieu, gioi_han_su_dung = :gioiHan,
                bat_dau_luc = :batDau, ket_thuc_luc = :ketThuc, dang_hoat_dong = :hoatDong
            WHERE ma_phieu_giam_gia = :ma
              AND NOT (BINARY ma_ap_dung = BINARY :maApDung AND loai_giam_gia=:loai AND gia_tri_giam=:giaTri
                AND muc_giam_toi_da <=> :toiDa AND don_toi_thieu=:toiThieu AND gioi_han_su_dung=:gioiHan
                AND bat_dau_luc=:batDau AND ket_thuc_luc=:ketThuc AND dang_hoat_dong=:hoatDong)
            """).param("ma", ma), phieu).update() == 1;
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

    public Optional<Phieu> timPhieuHopLe(String ma) {
        return jdbc.sql("""
            SELECT ma_phieu_giam_gia, loai_giam_gia, gia_tri_giam, muc_giam_toi_da, don_toi_thieu
            FROM phieu_giam_gia
            WHERE ma_ap_dung = ? AND dang_hoat_dong = TRUE
              AND bat_dau_luc <= UTC_TIMESTAMP(6) AND ket_thuc_luc > UTC_TIMESTAMP(6)
              AND so_luot_da_dung + so_luot_tam_giu < gioi_han_su_dung
            """).param(ma).query((rs, row) -> new Phieu(rs.getLong(1), rs.getString(2),
                rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getBigDecimal(5))).optional();
    }

    public Optional<String> lyDoKhongTheXemTruoc(String ma) {
        return jdbc.sql("""
            SELECT CASE
                WHEN dang_hoat_dong = FALSE THEN 'Mã giảm giá hiện không áp dụng.'
                WHEN ket_thuc_luc <= UTC_TIMESTAMP(6) THEN 'Mã giảm giá đã hết hạn.'
                WHEN bat_dau_luc > UTC_TIMESTAMP(6) THEN 'Mã giảm giá chưa đến thời gian áp dụng.'
                WHEN so_luot_da_dung + so_luot_tam_giu >= gioi_han_su_dung THEN 'Mã giảm giá đã hết lượt sử dụng.'
                ELSE 'Mã giảm giá chưa khả dụng. Vui lòng kiểm tra lại.'
            END
            FROM phieu_giam_gia WHERE ma_ap_dung = ?
            """).param(ma).query(String.class).optional();
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

    public enum Chieu {
        SAN_PHAM, DANH_MUC, THUONG_HIEU, NGUOI_DUNG;
        String bang() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    public record PhamVi(String slot, Integer gioiHanKhach, java.util.Map<Chieu, java.util.Set<Long>> dich) {
        public boolean choPhep(Chieu chieu, Long ma) {
            var ids = dich.get(chieu);
            return ids == null || ids.isEmpty() || (ma != null && ids.contains(ma));
        }
    }

    public PhamVi phamVi(long ma) {
        var dich = new java.util.EnumMap<Chieu, java.util.Set<Long>>(Chieu.class);
        for (var chieu : Chieu.values()) {
            String bang = chieu.bang();
            var ids = jdbc.sql("SELECT ma_" + bang + " FROM pham_vi_phieu_" + bang + " WHERE ma_phieu_giam_gia=?")
                .param(ma).query(Long.class).set();
            dich.put(chieu, ids);
        }
        return jdbc.sql("SELECT slot, gioi_han_khach FROM phieu_giam_gia WHERE ma_phieu_giam_gia=?")
            .param(ma).query((rs,n) -> new PhamVi(rs.getString(1), rs.getObject(2,Integer.class), dich)).single();
    }

    public boolean luuPhamVi(long ma, PhamVi moi) {
        var cu = phamVi(ma);
        if (cu.equals(moi)) return false;
        jdbc.sql("UPDATE phieu_giam_gia SET slot=:slot, gioi_han_khach=:quota WHERE ma_phieu_giam_gia=:ma")
            .param("slot",moi.slot()).param("quota",moi.gioiHanKhach(), Types.INTEGER).param("ma",ma).update();
        for (var chieu : Chieu.values()) {
            if (cu.dich().get(chieu).equals(moi.dich().get(chieu))) continue;
            String bang = "pham_vi_phieu_" + chieu.bang();
            jdbc.sql("DELETE FROM " + bang + " WHERE ma_phieu_giam_gia=?").param(ma).update();
            for (long id : moi.dich().get(chieu)) {
                jdbc.sql("INSERT INTO " + bang + " (ma_phieu_giam_gia,ma_" + chieu.bang() + ") VALUES (?,?)")
                    .params(ma,id).update();
            }
        }
        return true;
    }

    public boolean dichHopLe(PhamVi pv) {
        for (var entry : pv.dich().entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            String bang = entry.getKey().bang();
            String sql = "SELECT COUNT(*) FROM " + bang + " WHERE ma_" + bang + " IN (:ids)";
            if (entry.getKey() == Chieu.NGUOI_DUNG) sql += " AND vai_tro='CUSTOMER'";
            if (jdbc.sql(sql).param("ids",entry.getValue()).query(Integer.class).single() != entry.getValue().size()) return false;
        }
        return true;
    }

    public boolean quotaHopLe(long ma,Integer quota) {
        return quota == null || jdbc.sql("""
            SELECT NOT EXISTS (SELECT 1 FROM tam_giu_phieu_giam_gia g JOIN don_hang d USING(ma_don_hang)
            WHERE g.ma_phieu_giam_gia=? AND g.trang_thai IN ('HELD','CONSUMED')
            GROUP BY d.ma_nguoi_dung HAVING COUNT(*) > ?)
            """).params(ma,quota).query(Boolean.class).single();
    }

    public int luotKhach(long ma, long khach) {
        return jdbc.sql("""
            SELECT COUNT(*) FROM tam_giu_phieu_giam_gia g JOIN don_hang d USING (ma_don_hang)
            WHERE g.ma_phieu_giam_gia=? AND d.ma_nguoi_dung=? AND g.trang_thai IN ('HELD','CONSUMED')
            """).params(ma,khach).query(Integer.class).single();
    }

    public java.util.Set<Long> khoaMa(String hang, String ship) {
        var ids = jdbc.sql("SELECT ma_phieu_giam_gia FROM phieu_giam_gia WHERE ma_ap_dung IN (?,?) ORDER BY ma_phieu_giam_gia")
            .params(hang,ship).query(Long.class).list();
        var daKhoa = new java.util.HashSet<Long>();
        for (long id : ids) timTheoMa(id,true).ifPresent(phieu -> daKhoa.add(phieu.maPhieuGiamGia()));
        return daKhoa;
    }

    public boolean ghiSlot(long don, long ma, String slot, BigDecimal giam, boolean online) {
        if (online) tamGiu(ma);
        else ghiNhanSuDung(ma);
        // The preceding counter UPDATE holds the voucher row until the order transaction ends.
        int inserted = jdbc.sql("""
            INSERT INTO tam_giu_phieu_giam_gia(ma_don_hang,ma_phieu_giam_gia,slot,so_tien_giam,trang_thai)
            SELECT :don,:ma,:slot,:giam,:state FROM DUAL
            WHERE NOT EXISTS (
                SELECT 1 FROM phieu_giam_gia p WHERE p.ma_phieu_giam_gia=:ma AND p.gioi_han_khach IS NOT NULL
                AND p.gioi_han_khach <= (
                    SELECT COUNT(*) FROM tam_giu_phieu_giam_gia g
                    JOIN don_hang d ON d.ma_don_hang=g.ma_don_hang
                    WHERE g.ma_phieu_giam_gia=:ma AND g.trang_thai IN ('HELD','CONSUMED')
                    AND d.ma_nguoi_dung=(SELECT ma_nguoi_dung FROM don_hang WHERE ma_don_hang=:don)))
            """).param("don",don).param("ma",ma).param("slot",slot).param("giam",giam)
            .param("state",online ? "HELD" : "CONSUMED").update();
        return inserted == 1;
    }

    public List<Long> cacPhieu(long don) {
        return jdbc.sql("SELECT ma_phieu_giam_gia FROM tam_giu_phieu_giam_gia WHERE ma_don_hang=? ORDER BY ma_phieu_giam_gia")
            .param(don).query(Long.class).list();
    }

    public void traLuotCOD(long don) {
        for (long ma : cacPhieu(don)) {
            timTheoMa(ma,true);
            int n = jdbc.sql("""
                UPDATE tam_giu_phieu_giam_gia SET trang_thai='RELEASED'
                WHERE ma_don_hang=? AND ma_phieu_giam_gia=? AND trang_thai='CONSUMED'
                """).params(don,ma).update();
            if (n == 1 && !hoanLuot(ma)) throw new IllegalStateException("Không thể trả lượt mã giảm giá.");
        }
    }

    public record Phieu(long ma, String loai, BigDecimal giaTri, BigDecimal toiDa, BigDecimal toiThieu) {}
    public record CauHinh(String maApDung, String loaiGiamGia, BigDecimal giaTriGiam, BigDecimal mucGiamToiDa,
                          BigDecimal donToiThieu, int gioiHanSuDung, LocalDateTime batDauLuc,
                          LocalDateTime ketThucLuc, boolean dangHoatDong) {}
    public record ThongTin(long maPhieuGiamGia, CauHinh cauHinh, int soLuotDaDung, int soLuotTamGiu, String slot, Integer gioiHanKhach) {
        public int soLuotConLai() { return cauHinh.gioiHanSuDung() - soLuotDaDung - soLuotTamGiu; }
    }
}
