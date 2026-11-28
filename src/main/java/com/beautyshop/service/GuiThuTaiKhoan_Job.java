package com.beautyshop.service;

import java.net.ConnectException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(name = "app.account.mail-enabled", havingValue = "true")
class GuiThuTaiKhoan_Job {
    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final Email_Service email;

    GuiThuTaiKhoan_Job(JdbcClient jdbc, PlatformTransactionManager manager, Email_Service email) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(manager);
        this.email = email;
    }

    private record Thu(long id, String correlation, String diaChi, boolean mo, LocalDateTime utc, int lan) {}

    @Scheduled(initialDelay = 5000, fixedDelay = 5000)
    void chay() {
        // Hết lease có thể do worker đã gửi xong rồi chết trước khi ghi SENT.
        jdbc.sql("""
            UPDATE thu_trang_thai_tai_khoan SET trang_thai='UNKNOWN',ma_lease=NULL,het_lease=NULL
            WHERE trang_thai='SENDING' AND het_lease<=UTC_TIMESTAMP(6)
            """).update();
        var lease = UUID.randomUUID().toString();
        var thu = tx.execute(status -> nhan(lease));
        if (thu == null) return;
        String ketQua;
        try {
            email.guiTrangThai(UUID.fromString(thu.correlation()), thu.diaChi(), thu.mo(), thu.utc());
            ketQua = "SENT";
        } catch (RuntimeException ex) {
            ketQua = phanLoai(ex);
        }
        if (thu.lan() >= 4 && ketQua.equals("PENDING")) ketQua = "FAILED";
        int phut = switch (thu.lan()) { case 1 -> 1; case 2 -> 5; default -> 30; };
        jdbc.sql("""
            UPDATE thu_trang_thai_tai_khoan SET
                trang_thai=CASE WHEN het_lease>UTC_TIMESTAMP(6) THEN ? ELSE 'UNKNOWN' END,
                ma_lease=NULL,het_lease=NULL,
                gui_tu=TIMESTAMPADD(MINUTE,?,UTC_TIMESTAMP(6))
            WHERE ma_thu=? AND trang_thai='SENDING' AND ma_lease=?
            """).params(ketQua, phut, thu.id(), lease).update();
    }

    private Thu nhan(String lease) {
        var id = jdbc.sql("""
            SELECT t.ma_thu FROM thu_trang_thai_tai_khoan t
            WHERE t.trang_thai='PENDING' AND t.so_lan_gui<4 AND t.gui_tu<=UTC_TIMESTAMP(6)
            AND NOT EXISTS (SELECT 1 FROM thu_trang_thai_tai_khoan cu
                WHERE cu.ma_nguoi_dung=t.ma_nguoi_dung AND cu.ma_thu<t.ma_thu
                AND cu.trang_thai IN ('PENDING','SENDING','UNKNOWN'))
            ORDER BY t.gui_tu,t.ma_thu LIMIT 1
            """).query(Long.class).optional();
        if (id.isEmpty()) return null;
        int rows = jdbc.sql("""
            UPDATE thu_trang_thai_tai_khoan SET trang_thai='SENDING',so_lan_gui=so_lan_gui+1,
                ma_lease=?,het_lease=TIMESTAMPADD(MINUTE,2,UTC_TIMESTAMP(6))
            WHERE ma_thu=? AND trang_thai='PENDING' AND gui_tu<=UTC_TIMESTAMP(6) AND so_lan_gui<4
            """).params(lease, id.get()).update();
        if (rows == 0) return null;
        return jdbc.sql("""
            SELECT ma_thu AS id,ma_thao_tac AS correlation,nguoi_nhan AS diaChi,mo_khoa AS mo,thoi_diem AS utc,so_lan_gui AS lan
            FROM thu_trang_thai_tai_khoan WHERE ma_thu=?
            """).param(id.get()).query(Thu.class).single();
    }

    static String phanLoai(RuntimeException ex) {
        // Một event chỉ có một người nhận. Chỉ retry khi có bằng chứng bị từ chối.
        Throwable cause = ex;
        if (ex instanceof MailSendException mail && mail.getMessageExceptions().length == 1) {
            cause = mail.getMessageExceptions()[0];
        }
        for (Throwable err = cause; err != null; err = err.getCause()) {
            if (err instanceof MailAuthenticationException) return "FAILED";
            if (err instanceof ConnectException) return "PENDING";
            if (err instanceof SMTPAddressFailedException addr) return maPhanHoi(addr.getReturnCode());
            if (err instanceof SMTPSendFailedException smtp) {
                String cmd = smtp.getCommand();
                if (cmd != null && (cmd.startsWith("MAIL FROM:") || cmd.startsWith("RCPT TO:")
                        || cmd.equals("DATA") || cmd.equals("."))) return maPhanHoi(smtp.getReturnCode());
            }
        }
        return "UNKNOWN";
    }

    private static String maPhanHoi(int code) {
        if (code >= 400 && code < 500) return "PENDING";
        if (code >= 500 && code < 600) return "FAILED";
        return "UNKNOWN";
    }
}
