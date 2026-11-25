package com.beautyshop.repository;

import com.beautyshop.payment.MomoGateway;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ThanhToanMomo_Repository {
    static final String CO_GIU_HANG_QUA_HAN = """
        EXISTS (SELECT 1 FROM tam_giu_ton_kho t WHERE t.ma_don_hang = d.ma_don_hang
            AND t.trang_thai = 'HELD' AND t.het_han_luc <= %s)
        """;
    private static final String SELECT = """
        SELECT tt.ma_don_hang, d.ma_don_hang_hien_thi, tt.ma_yeu_cau, tt.so_tien, tt.cong_thanh_toan,
               tt.trang_thai, tt.ma_giao_dich_cong
        FROM lan_thanh_toan tt JOIN don_hang d ON d.ma_don_hang = tt.ma_don_hang
        WHERE tt.cong_thanh_toan IN ('MOMO', 'MOMO_MOCK') AND
        """;

    private final JdbcClient jdbc;

    ThanhToanMomo_Repository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public void tao(long maDon, String requestId, BigDecimal tong, String gateway) {
        jdbc.sql("""
            INSERT INTO lan_thanh_toan (ma_don_hang, ma_yeu_cau, cong_thanh_toan, so_tien, trang_thai, ngay_tao)
            VALUES (?, ?, ?, ?, 'PENDING', UTC_TIMESTAMP(6))
            """).params(maDon, requestId, gateway, tong).update();
    }

    public Optional<LanThanhToan> theoRequest(String requestId) {
        return tim(SELECT + "tt.ma_yeu_cau = ?", requestId);
    }

    public Optional<LanThanhToan> moiNhat(long maDon) {
        return tim(SELECT + "tt.ma_don_hang = ? ORDER BY tt.ma_lan_thanh_toan DESC LIMIT 1", maDon);
    }

    private Optional<LanThanhToan> tim(String sql, Object giaTri) {
        return jdbc.sql(sql).param(giaTri).query((rs, row) -> new LanThanhToan(
            new MomoGateway.Request(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBigDecimal(4).longValueExact()),
            rs.getString(5), rs.getString(6), rs.getString(7))).optional();
    }

    public boolean conHan(long maDon) {
        return jdbc.sql("""
            SELECT COALESCE(MIN(het_han_luc) > UTC_TIMESTAMP(6), FALSE)
            FROM tam_giu_ton_kho WHERE ma_don_hang = ? AND trang_thai = 'HELD'
            """).param(maDon).query(Boolean.class).single();
    }

    public List<Long> dangGiu(long maDon) {
        return jdbc.sql("""
            SELECT ma_bien_the FROM tam_giu_ton_kho WHERE ma_don_hang = ? AND trang_thai = 'HELD'
            ORDER BY ma_bien_the
            """).param(maDon).query(Long.class).list();
    }

    public List<Long> donQuaHan(String gateway, long sauMaDon) {
        return jdbc.sql("""
            SELECT d.ma_don_hang FROM don_hang d
            WHERE d.ma_don_hang > ? AND d.phuong_thuc_thanh_toan = 'MOMO' AND d.trang_thai = 'PENDING_PAYMENT'
              AND %s
              AND EXISTS (SELECT 1 FROM lan_thanh_toan l WHERE l.ma_don_hang = d.ma_don_hang
                  AND l.cong_thanh_toan = ?)
            ORDER BY d.ma_don_hang LIMIT 3
            """.formatted(CO_GIU_HANG_QUA_HAN.formatted("UTC_TIMESTAMP(6)")))
            .params(sauMaDon, gateway).query(Long.class).list();
    }

    public void capNhat(String requestId, String trangThai, Long transId) {
        String giaoDich = transId == null ? null : Long.toString(transId);
        jdbc.sql("""
            UPDATE lan_thanh_toan SET trang_thai = :trangThai, ma_giao_dich_cong = :giaoDich,
                ngay_thanh_toan = CASE WHEN :trangThai IN ('PAID', 'REFUND_PENDING') THEN UTC_TIMESTAMP(6)
                                          ELSE ngay_thanh_toan END
            WHERE ma_yeu_cau = :requestId
            """).param("trangThai", trangThai).param("giaoDich", giaoDich, java.sql.Types.VARCHAR)
            .param("requestId", requestId).update();
    }

    public long soLanCanHoan(long maDon) {
        return jdbc.sql("""
            SELECT count(*) FROM lan_thanh_toan WHERE ma_don_hang = ? AND trang_thai = 'REFUND_PENDING'
            """).param(maDon).query(Long.class).single();
    }

    public Optional<String> trangThaiHienThi(long maDon) {
        return jdbc.sql("""
            SELECT tt.trang_thai FROM lan_thanh_toan tt
            WHERE tt.ma_don_hang = ? AND tt.cong_thanh_toan IN ('MOMO', 'MOMO_MOCK')
            ORDER BY (tt.trang_thai = 'PAID') DESC, tt.ma_lan_thanh_toan DESC LIMIT 1
            """).param(maDon).query(String.class).optional();
    }

    public record LanThanhToan(MomoGateway.Request request, String gateway, String trangThai, String transId) {}
}
