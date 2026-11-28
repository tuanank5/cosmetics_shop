package com.beautyshop.service;

import com.beautyshop.model.NguoiDung;
import com.beautyshop.model.YeuCauDatLaiMatKhau;
import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.repository.YeuCauDatLaiMatKhau_Repository;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class MatKhau_Service {
    public enum KetQuaGui { DA_GUI, KHONG_DU_DIEU_KIEN, LOI_GUI }
    public enum LyDoHoTro { KHACH_YEU_CAU, HO_TRO_BAO_MAT }
    private record ThuDatLai(long requestId, long userId, String email, String token, String correlation) {}

    private static final Logger log = LoggerFactory.getLogger(MatKhau_Service.class);
    private static final DefaultRedisScript<Long> GIOI_HAN = new DefaultRedisScript<>("""
        local n = redis.call('INCR', KEYS[1])
        if n == 1 then redis.call('EXPIRE', KEYS[1], 60) end
        return n
        """, Long.class);

    private final NguoiDung_Repository users;
    private final YeuCauDatLaiMatKhau_Repository requests;
    private final NhatKyQuanTri_Service audit;
    private final Email_Service mail;
    private final PasswordEncoder encoder;
    private final DanhTinhOAuth_Service identities;
    private final StringRedisTemplate redis;
    private final TaskExecutor executor;
    private final TransactionTemplate transactions;
    private final String publicUrl;
    private final SecureRandom random = new SecureRandom();

    MatKhau_Service(NguoiDung_Repository users, YeuCauDatLaiMatKhau_Repository requests,
                    NhatKyQuanTri_Service audit, Email_Service mail, PasswordEncoder encoder,
                    DanhTinhOAuth_Service identities, StringRedisTemplate redis,
                    @Qualifier("passwordResetExecutor") TaskExecutor executor, PlatformTransactionManager tx,
                    @Value("${beautyshop.auth.public-url:}") String publicUrl) {
        this.users = users;
        this.requests = requests;
        this.audit = audit;
        this.mail = mail;
        this.encoder = encoder;
        this.identities = identities;
        this.redis = redis;
        this.executor = executor;
        this.transactions = new TransactionTemplate(tx);
        this.transactions.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.publicUrl = publicUrl.strip().replaceAll("/+$", "");
    }

    public void quenMatKhau(String email, String sourceAddress) {
        if (email == null || email.length() > 190 || !email.matches("[^@\\s]+@[^@\\s]+")) {
            return;
        }
        String normalized = NguoiDung_Service.chuanHoaEmail(email);
        try {
            executor.execute(() -> {
                try {
                    Long count = redis.execute(GIOI_HAN, List.of("beautyshop:reset:source:" + bamHex(sourceAddress)));
                    if (count == null || count > 10 || !Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(
                            "beautyshop:reset:email:" + bamHex(normalized), "1", Duration.ofMinutes(1)))) {
                        return;
                    }
                    var thu = transactions.execute(status -> users.timMaTheoEmail(normalized)
                        .flatMap(users::khoa)
                        .filter(user -> normalized.equals(user.getEmail()))
                        .map(user -> taoYeuCau(user, null, null)).orElse(null));
                    gui(thu);
                } catch (RuntimeException failure) {
                    log.warn("Không xử lý được yêu cầu đặt lại mật khẩu: {}", failure.getClass().getSimpleName());
                }
            });
        } catch (org.springframework.core.task.TaskRejectedException failure) {
            log.warn("Hàng đợi đặt lại mật khẩu đã đầy.");
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    public KetQuaGui adminYeuCau(long adminId, long version, long target, LyDoHoTro reason) {
        if (adminId == target || reason == null) {
            return KetQuaGui.KHONG_DU_DIEU_KIEN;
        }
        var thu = transactions.execute(status -> {
            var first = users.khoa(Math.min(adminId, target)).orElse(null);
            var second = users.khoa(Math.max(adminId, target)).orElse(null);
            var admin = adminId < target ? first : second;
            var customer = adminId < target ? second : first;
            if (admin == null || !admin.isKichHoat() || admin.getVaiTro() != NguoiDung.VaiTro.ADMIN
                || admin.getPhienBanXacThuc() != version) {
                throw new AccessDeniedException("Phiên quản trị đã hết hiệu lực.");
            }
            return customer == null ? null : taoYeuCau(customer, adminId, reason);
        });
        return gui(thu);
    }

    private ThuDatLai taoYeuCau(NguoiDung user, Long admin, LyDoHoTro reason) {
        var now = utc();
        long id = user.getMaNguoiDung();
        if (!duDieuKien(user) || !linkHopLe()
            || requests.existsByNguoiDung_MaNguoiDungAndNgayTaoAfter(id, now.minusMinutes(1))) {
            return null;
        }
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        String correlation = UUID.randomUUID().toString();
        requests.hetHanCacLink(id, now);
        var request = requests.saveAndFlush(new YeuCauDatLaiMatKhau(user, bam(token), now,
            admin == null ? "SELF" : "ADMIN", correlation));
        audit.ghi(admin, admin == null ? "ANONYMOUS" : "ADMIN",
            admin == null ? "PASSWORD_RESET_REQUESTED" : "ADMIN_PASSWORD_RESET_REQUESTED",
            "ACCOUNT", id, "SUCCESS", reason == null ? null : reason.name(), correlation);
        return new ThuDatLai(request.getMaYeuCau(), id, user.getEmail(), token, correlation);
    }

    private KetQuaGui gui(ThuDatLai thu) {
        if (thu == null) {
            return KetQuaGui.KHONG_DU_DIEU_KIEN;
        }
        boolean sent;
        try {
            mail.guiLinkDatLai(thu.email(), publicUrl + "/reset-password?token=" + thu.token());
            sent = true;
        } catch (RuntimeException failure) {
            sent = false;
        }
        final boolean delivered = sent;
        transactions.executeWithoutResult(status -> {
            if (!delivered) {
                users.khoa(thu.userId());
                requests.hetHanLink(thu.requestId(), utc());
            }
            audit.ghi(null, "SYSTEM", delivered ? "PASSWORD_RESET_EMAIL_SENT" : "PASSWORD_RESET_EMAIL_FAILED",
                "RESET_REQUEST", thu.requestId(), delivered ? "SUCCESS" : "FAILED", null, thu.correlation());
        });
        return sent ? KetQuaGui.DA_GUI : KetQuaGui.LOI_GUI;
    }

    public boolean datLai(String token, String password, String xacNhan) {
        if (!dinhDangToken(token)) {
            return false;
        }
        byte[] hash = bam(token);
        String email = transactions.execute(status -> {
            var owner = requests.timChuSoHuu(hash).orElse(null);
            if (owner == null) {
                return null;
            }
            var user = users.khoa(owner).orElseThrow();
            var request = requests.findByTokenBam(hash).orElseThrow();
            var now = utc();
            if (!duDieuKien(user) || !request.hopLe(now, user.getPhienBanXacThuc())) {
                return null;
            }
            ganMatKhauMoi(user, password, xacNhan);
            request.tieuThu(now);
            audit.ghi(owner, "CUSTOMER", "PASSWORD_RESET_COMPLETED", "ACCOUNT", owner,
                "SUCCESS", null, request.getCorrelationId());
            audit.thuHoiPhien(owner, request.getCorrelationId());
            return user.getEmail();
        });
        if (email != null) {
            try {
                mail.baoDaDoiMatKhau(email);
            } catch (RuntimeException failure) {
                log.warn("Không gửi được thư thông báo thay đổi mật khẩu.");
            }
        }
        return email != null;
    }

    public boolean linkConHieuLuc(String token) {
        return dinhDangToken(token) && requests.demLinkHopLe(bam(token), utc(), NguoiDung.VaiTro.CUSTOMER) == 1;
    }

    @Transactional
    public void doi(long id, long version, String currentPassword, String password, String xacNhan, boolean oauthProof) {
        boolean coTheThu = currentPassword != null && !currentPassword.isEmpty() && choPhepThuMatKhau(id);
        var user = identities.khoaCustomer(id, version);
        boolean first = user.getMatKhauBam() == null;
        boolean authorized = first ? oauthProof && user.getNgayXacMinhEmail() != null
            : coTheThu && encoder.matches(currentPassword, user.getMatKhauBam());
        if (!authorized) {
            throw new AccessDeniedException("Xác minh lại để thay đổi mật khẩu.");
        }
        ganMatKhauMoi(user, password, xacNhan);
        String correlation = UUID.randomUUID().toString();
        audit.ghi(id, "CUSTOMER", first ? "PASSWORD_CREATED" : "PASSWORD_CHANGED",
            "ACCOUNT", id, "SUCCESS", null, correlation);
        audit.thuHoiPhien(id, correlation);
    }

    public static final class MatKhauMoiKhongHopLe extends RuntimeException {
        private MatKhauMoiKhongHopLe(String message) { super(message); }
    }

    private void ganMatKhauMoi(NguoiDung user, String value, String xacNhan) {
        user.doiMatKhau(bamMatKhauMoi(value, xacNhan, user.getMatKhauBam(), encoder));
    }

    static String bamMatKhauMoi(String value, String xacNhan, String bamHienTai, PasswordEncoder encoder) {
        if (!matKhauMoiHopLe(value, xacNhan)) {
            throw new MatKhauMoiKhongHopLe("Mật khẩu cần từ 12 ký tự, tối đa 72 byte và khớp xác nhận.");
        }
        if (bamHienTai != null && encoder.matches(value, bamHienTai)) {
            throw new MatKhauMoiKhongHopLe("Hãy chọn mật khẩu mới khác mật khẩu hiện tại.");
        }
        return encoder.encode(value);
    }

    public static boolean matKhauMoiHopLe(String value, String xacNhan) {
        return value != null && value.equals(xacNhan) && !value.isBlank() && value.codePointCount(0, value.length()) >= 12
            && value.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    public boolean choPhepThuMatKhau(long id) {
        try {
            Long count = redis.execute(GIOI_HAN, List.of("beautyshop:security:password:" + id));
            return count != null && count <= 10;
        } catch (org.springframework.dao.DataAccessException failure) {
            log.warn("Chưa thể kiểm tra giới hạn xác thực lại bằng mật khẩu.");
            return false;
        }
    }

    public static boolean dinhDangToken(String value) {
        return value != null && value.matches("[A-Za-z0-9_-]{43}");
    }

    private static boolean duDieuKien(NguoiDung user) {
        return duDieuKien(user.isKichHoat(), user.getVaiTro(), user.getMatKhauBam() != null, user.getNgayXacMinhEmail() != null);
    }

    public static boolean duDieuKien(boolean kichHoat, NguoiDung.VaiTro vaiTro, boolean coMatKhau, boolean daXacMinhEmail) {
        return kichHoat && vaiTro == NguoiDung.VaiTro.CUSTOMER && coMatKhau && daXacMinhEmail;
    }

    private boolean linkHopLe() {
        try {
            var uri = URI.create(publicUrl);
            return uri.getHost() != null && uri.getUserInfo() == null && uri.getQuery() == null
                && uri.getFragment() == null && ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())
                    && ("localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost())));
        } catch (IllegalArgumentException failure) {
            return false;
        }
    }

    private static String bamHex(String value) { return HexFormat.of().formatHex(bam(value)); }

    private static byte[] bam(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static LocalDateTime utc() { return LocalDateTime.now(ZoneOffset.UTC); }
}
