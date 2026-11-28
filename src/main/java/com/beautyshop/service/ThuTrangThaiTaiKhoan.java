package com.beautyshop.service;

import com.beautyshop.model.NguoiDung;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Repository;

@Repository
public class ThuTrangThaiTaiKhoan {
    public enum TrangThai {
        UNVERIFIED("Chưa gửi · email chưa xác minh"), PENDING("Chờ gửi"), SENDING("Đang gửi"),
        SENT("Máy chủ thư đã tiếp nhận"), FAILED("Gửi thất bại"), UNKNOWN("Chờ đối soát");

        private final String nhan;
        TrangThai(String nhan) { this.nhan = nhan; }
        public String getNhan() { return nhan; }
    }

    public record TomTat(long dich, long admin, LocalDateTime utc, LyDoTaiKhoan lyDo, TrangThai trangThai, String correlation, int soLanGui) {}
    private static final String TOM_TAT = """
        SELECT ma_nguoi_dung AS dich, ma_admin AS admin, thoi_diem AS utc, ly_do AS lyDo,
               trang_thai AS trangThai, ma_thao_tac AS correlation, so_lan_gui AS soLanGui FROM thu_trang_thai_tai_khoan
        """;
    private final JdbcClient jdbc;

    ThuTrangThaiTaiKhoan(JdbcClient jdbc) { this.jdbc = jdbc; }

    enum BienNhan { MOI, DA_GHI, XUNG_DOT }

    BienNhan bienNhan(UUID id, long admin, long dich, long version, LyDoTaiKhoan lyDo) {
        return jdbc.sql("""
            SELECT ma_admin, ma_nguoi_dung, phien_ban_truoc, ly_do
            FROM thu_trang_thai_tai_khoan WHERE ma_thao_tac=?
            """).param(id.toString()).query((r, n) ->
                r.getLong(1) == admin && r.getLong(2) == dich && r.getLong(3) == version
                && lyDo.name().equals(r.getString(4)) ? BienNhan.DA_GHI : BienNhan.XUNG_DOT)
            .optional().orElseGet(() -> {
                boolean daThatBai = jdbc.sql("""
                    SELECT COUNT(*) FROM nhat_ky_quan_tri WHERE correlation_id=?
                    AND hanh_dong IN ('ACCOUNT_LOCK','ACCOUNT_UNLOCK')
                    """).param(id.toString()).query(Long.class).single() > 0;
                return daThatBai ? BienNhan.XUNG_DOT : BienNhan.MOI;
            });
    }

    void tao(UUID id, long admin, NguoiDung dich, long version, LyDoTaiKhoan lyDo) {
        boolean verified = dich.getNgayXacMinhEmail() != null;
        jdbc.sql("""
            INSERT INTO thu_trang_thai_tai_khoan
                (ma_thao_tac,ma_admin,ma_nguoi_dung,mo_khoa,phien_ban_truoc,ly_do,nguoi_nhan,
                 thoi_diem,trang_thai,gui_tu)
            VALUES (:id,:admin,:dich,:mo,:version,:lyDo,:email,UTC_TIMESTAMP(6),:trangThai,UTC_TIMESTAMP(6))
            """).param("id", id.toString()).param("admin", admin).param("dich", dich.getMaNguoiDung())
            .param("mo", lyDo.isMoKhoa()).param("version", version).param("lyDo", lyDo.name())
            .param("email", verified ? dich.getEmail() : null, Types.VARCHAR)
            .param("trangThai", verified ? "PENDING" : "UNVERIFIED").update();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Map<Long, TomTat> moiNhat(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return jdbc.sql(TOM_TAT + """
            WHERE ma_thu IN (SELECT MAX(ma_thu) FROM thu_trang_thai_tai_khoan
                WHERE ma_nguoi_dung IN (:ids) GROUP BY ma_nguoi_dung)
            """).param("ids", ids).query(TomTat.class).list().stream()
            .collect(Collectors.toMap(TomTat::dich, Function.identity()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<TomTat> lichSu(long dich) {
        return jdbc.sql(TOM_TAT + " WHERE ma_nguoi_dung=? ORDER BY ma_thu DESC LIMIT 50")
            .param(dich).query(TomTat.class).list();
    }
}
