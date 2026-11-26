package com.beautyshop.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class EmailVerification_Service {
    private static final DefaultRedisScript<Long> KIEM_TRA = new DefaultRedisScript<>("""
        local expected = redis.call('HGET', KEYS[1], 'code')
        if not expected then return 0 end
        local count = redis.call('HINCRBY', KEYS[1], 'attempts', 1)
        if expected == ARGV[1] and count <= 5 then
            redis.call('DEL', KEYS[1])
            return 1
        end
        if count >= 5 then redis.call('DEL', KEYS[1]) end
        return 0
        """, Long.class);

    private static final DefaultRedisScript<Long> LUU = new DefaultRedisScript<>("""
        redis.call('HSET', KEYS[1], 'code', ARGV[1])
        redis.call('EXPIRE', KEYS[1], 300)
        return 1
        """, Long.class);

    private final StringRedisTemplate redis;
    private final Email_Service emailService;
    private final SecureRandom random = new SecureRandom();
    private final byte[] key = new byte[32];

    public EmailVerification_Service(StringRedisTemplate redis, Email_Service emailService) {
        this.redis = redis;
        this.emailService = emailService;
        random.nextBytes(key);
    }

    public String guiOTP(String email, String purpose) {
        String gioiHan = "beautyshop:otp:cooldown:" + purpose + ":" + hmac(email);
        if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(gioiHan, "1", Duration.ofSeconds(60)))) {
            throw new IllegalStateException("Vui lòng đợi 60 giây trước khi gửi lại mã OTP.");
        }
        String challenge = UUID.randomUUID().toString();
        String otp = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        redis.execute(LUU, List.of(redisKey(challenge)), hmac(challenge + ":" + otp));
        try {
            if ("register".equals(purpose)) {
                emailService.guiMaOTP(email, otp);
            } else {
                emailService.guiMaBaoMat(email, otp);
            }
        } catch (RuntimeException failure) {
            xoaOTP(challenge);
            throw failure;
        }
        return challenge;
    }

    public boolean xacThucOTP(String challenge, String otp) {
        return challenge != null && otp != null && otp.matches("[0-9]{6}")
            && Long.valueOf(1).equals(redis.execute(KIEM_TRA, List.of(redisKey(challenge)),
                hmac(challenge + ":" + otp)));
    }

    public void xoaOTP(String challenge) {
        if (challenge != null) {
            redis.delete(redisKey(challenge));
        }
    }

    private String hmac(String value) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("Không thể tạo mã xác minh.", failure);
        }
    }

    private static String redisKey(String challenge) {
        return "beautyshop:otp:challenge:" + challenge;
    }
}
