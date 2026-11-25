package com.beautyshop.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class XacThucEmail_Service {
    private static final Duration HAN_MA = Duration.ofMinutes(5);
    private static final Duration CHO_GUI_LAI = Duration.ofSeconds(60);
    private static final DefaultRedisScript<Long> XAC_THUC =
        new DefaultRedisScript<>();

    static {
        XAC_THUC.setLocation(new ClassPathResource("redis/xac-thuc-otp.lua"));
        XAC_THUC.setResultType(Long.class);
    }

    private final StringRedisTemplate redis;
    private final JavaMailSender mailSender;
    private final String diaChiGui;
    private final SecureRandom random = new SecureRandom();

    XacThucEmail_Service(StringRedisTemplate redis, JavaMailSender mailSender,
                         @Value("${app.auth.otp.from}") String diaChiGui) {
        this.redis = redis;
        this.mailSender = mailSender;
        this.diaChiGui = diaChiGui;
    }

    public boolean guiMa(UUID maDangKy, String email) {
        String khoaGui = "beautyshop:register:send:" + bam(email);
        if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(khoaGui, "1", CHO_GUI_LAI))) {
            return false;
        }

        String ma = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        var thu = new SimpleMailMessage();
        thu.setFrom(diaChiGui);
        thu.setTo(email);
        thu.setSubject("BeautyShop - Xác thực email đăng ký");
        thu.setText("Mã xác thực đăng ký BeautyShop của bạn là: " + ma
            + "\nMã có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này.");
        mailSender.send(thu);
        redis.opsForValue().set(khoaMa(maDangKy), bam(maDangKy + ":" + ma) + ":0", HAN_MA);
        return true;
    }

    public boolean xacThuc(UUID maDangKy, String ma) {
        return Long.valueOf(1).equals(redis.execute(XAC_THUC, List.of(khoaMa(maDangKy)), bam(maDangKy + ":" + ma)));
    }

    private static String khoaMa(UUID maDangKy) {
        return "beautyshop:register:code:" + bam(maDangKy.toString());
    }

    private static String bam(String giaTri) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(giaTri.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
