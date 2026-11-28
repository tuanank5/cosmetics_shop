package com.beautyshop.service;

import com.beautyshop.security.PhienNguoiDung;
import java.sql.Types;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class SuKienQuanTri_Service {
    enum HanhDong { ORDER_STATUS, SKU_STOCK, CATALOG_CREATE, CATALOG_UPDATE, CATALOG_ACTIVE,
                    VOUCHER_CREATE, VOUCHER_UPDATE, VOUCHER_DELETE, AUDIT_PREVIEW, AUDIT_PURGE, ACCOUNT_LOCK, ACCOUNT_UNLOCK,
                    RETURN_APPROVE, RETURN_REJECT, RETURN_RECEIVE, RETURN_INSPECT, RETURN_CONFIRM, RETURN_RECONCILE, RETURN_CLOSE }

    private final JdbcClient jdbc;
    private final TransactionTemplate sauRollback;

    SuKienQuanTri_Service(JdbcClient jdbc, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        sauRollback = new TransactionTemplate(manager);
        sauRollback.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    static PhienNguoiDung phienAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof PhienNguoiDung phien)
                || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new AccessDeniedException("Yêu cầu phiên quản trị.");
        }
        return phien;
    }

    record SuKien(long actor, HanhDong action, String loai, Long ma, UUID correlation) {}

    SuKien batDau(HanhDong action, String loai, Long ma) {
        return batDau(action, loai, ma, UUID.randomUUID());
    }

    SuKien batDau(HanhDong action, String loai, Long ma, UUID correlation) {
        var e = new SuKien(phienAdmin().getMaNguoiDung(), action, loai, ma, correlation);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    sauRollback.executeWithoutResult(tx -> ghi(e, ma, "FAILED", null, null, "ROLLBACK"));
                }
            }
        });
        return e;
    }

    void thanhCong(SuKien e, long ma, Object truoc, Object sau) {
        ghi(e, ma, "SUCCESS", snapshot(truoc), snapshot(sau), null);
    }

    void thanhCongTaiKhoan(SuKien e, long ma, boolean moKhoa, LyDoTaiKhoan lyDo) {
        ghi(e, ma, "SUCCESS", snapshot(!moKhoa), snapshot(moKhoa), lyDo.name());
    }

    static String snapshot(Object v) {
        return switch (v) {
            case null -> null;
            case Boolean b -> b.toString();
            case Integer n -> n.toString();
            case Long n -> n.toString();
            case com.beautyshop.order.ReturnStatus status -> status.name();
            case com.beautyshop.order.OrderStatus status -> status.name();
            default -> throw new IllegalArgumentException("Trường ngoài danh sách audit.");
        };
    }

    private void ghi(SuKien e, Long ma, String result, String truoc, String sau, String lyDo) {
        // A rolled-back create has no durable document. Reference the acting account.
        jdbc.sql("""
            INSERT INTO nhat_ky_quan_tri (ma_nguoi_thuc_hien,vai_tro_nguoi_thuc_hien,hanh_dong,
                loai_doi_tuong,ma_doi_tuong,thoi_gian,ket_qua,ly_do,correlation_id,truoc,sau)
            VALUES (:actor,'ADMIN',:action,:loai,:ma,UTC_TIMESTAMP(6),:result,:lyDo,:corr,:truoc,:sau)
            """).param("actor", e.actor()).param("action", e.action().name())
            .param("loai", ma == null ? "ADMIN" : e.loai()).param("ma", ma == null ? e.actor() : ma)
            .param("result", result).param("lyDo", lyDo, Types.VARCHAR)
            .param("corr", e.correlation().toString()).param("truoc", truoc, Types.VARCHAR)
            .param("sau", sau, Types.VARCHAR).update();
    }
}
