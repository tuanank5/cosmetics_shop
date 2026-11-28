package com.beautyshop.service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DonNhatKy_Service {
    private static final String DU_DIEU_KIEN = """
        thoi_gian < :moc
        AND EXISTS (SELECT 1 FROM audit_purge_clearance c WHERE c.batch_id=:batchId
            AND c.audit_id=nhat_ky_quan_tri.ma_nhat_ky AND c.expires_utc > UTC_TIMESTAMP(6))
        AND NOT EXISTS (SELECT 1 FROM bao_luu_nhat_ky b WHERE b.ma_nhat_ky=nhat_ky_quan_tri.ma_nhat_ky)
        AND (loai_doi_tuong <> 'ORDER' OR (
          NOT EXISTS (SELECT 1 FROM lan_thanh_toan t WHERE t.ma_don_hang=nhat_ky_quan_tri.ma_doi_tuong
            AND t.trang_thai IN ('PENDING','REFUND_PENDING'))
          AND NOT EXISTS (SELECT 1 FROM yeu_cau_doi_tra r WHERE r.ma_don_hang=nhat_ky_quan_tri.ma_doi_tuong
            AND r.trang_thai NOT IN ('COMPLETED','REJECTED'))))
        """;
    private final JdbcClient jdbc;
    private final SuKienQuanTri_Service audit;
    private final Set<Long> vanHanh;
    private final boolean coNguonBaoLuu;

    DonNhatKy_Service(JdbcClient jdbc, SuKienQuanTri_Service audit,
                      @Value("${app.audit.operator-ids:}") Set<Long> vanHanh,
                      @Value("${app.audit.clearance-source-enabled:false}") boolean coNguonBaoLuu) {
        this.coNguonBaoLuu = coNguonBaoLuu;
        this.jdbc = jdbc;
        this.audit = audit;
        this.vanHanh = vanHanh;
    }

    public record KetQua(long duDieuKien, int daDon) {}

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public KetQua chay(UUID correlation, boolean dryRun, int ngay, int batch) {
        var phien = SuKienQuanTri_Service.phienAdmin();
        long actor = phien.getMaNguoiDung();
        if (!vanHanh.contains(actor)) throw new AccessDeniedException("Chưa được phân công vận hành.");
        if (ngay < 180 || batch < 1 || batch > 500) {
            throw new IllegalArgumentException("Retention hoặc kích thước batch ngoài giới hạn.");
        }
        jdbc.sql("""
            SELECT ma_nguoi_dung FROM nguoi_dung WHERE ma_nguoi_dung=? AND vai_tro='ADMIN'
              AND kich_hoat AND phien_ban_xac_thuc=? FOR UPDATE
            """).params(actor,phien.getPhienBanXacThuc()).query(Long.class).optional()
            .orElseThrow(() -> new AccessDeniedException("Phiên vận hành đã hết hiệu lực."));
        if (!coNguonBaoLuu) throw new IllegalStateException("Chưa có nguồn xác nhận hồ sơ đủ điều kiện dọn.");
        var action = dryRun ? SuKienQuanTri_Service.HanhDong.AUDIT_PREVIEW : SuKienQuanTri_Service.HanhDong.AUDIT_PURGE;
        var cu = jdbc.sql("""
            SELECT ma_nguoi_thuc_hien, ket_qua, truoc, sau FROM nhat_ky_quan_tri
            WHERE correlation_id=? AND hanh_dong=?
            """).params(correlation.toString(), action.name()).query((rs, n) -> {
                if (rs.getLong(1) != actor || !"SUCCESS".equals(rs.getString(2))) {
                    throw new IllegalStateException("Tác vụ đã kết thúc. Kiểm tra nhật ký trước khi chạy batch mới.");
                }
                return new KetQua(rs.getLong(3), rs.getInt(4));
            }).optional();
        if (cu.isPresent()) return cu.get();
        var event = audit.batDau(action, "ADMIN", actor, correlation);
        LocalDateTime moc = jdbc.sql("SELECT UTC_TIMESTAMP(6)").query(LocalDateTime.class).single().minusDays(ngay);
        if (moc.getYear() < 1000) moc = LocalDateTime.of(1000,1,1,0,0); // MariaDB DATETIME minimum
        long dem = jdbc.sql("SELECT COUNT(*) FROM nhat_ky_quan_tri WHERE " + DU_DIEU_KIEN)
            .param("batchId",correlation.toString()).param("moc", moc).query(Long.class).single();
        int daDon = dryRun ? 0 : jdbc.sql("DELETE FROM nhat_ky_quan_tri WHERE " + DU_DIEU_KIEN
            + " ORDER BY thoi_gian, ma_nhat_ky LIMIT :batch").param("batchId",correlation.toString()).param("moc", moc).param("batch", batch).update();
        audit.thanhCong(event, actor, dem, daDon);
        return new KetQua(dem, daDon);
    }
}
