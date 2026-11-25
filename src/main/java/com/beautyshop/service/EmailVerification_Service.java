
package com.beautyshop.service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class EmailVerification_Service {

    private final StringRedisTemplate redisTemplate;
    private final Email_Service emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_TTL = Duration.ofSeconds(60);
    private static final Duration ATTEMPT_TTL = Duration.ofMinutes(5);

    private static final int MAX_ATTEMPTS = 5;

    public EmailVerification_Service(
            StringRedisTemplate redisTemplate,
            Email_Service emailService) {
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
    }

    private String keyOTP(String email) {
        return "beautyshop:otp:" + email;
    }

    private String keyResend(String email) {
        return "beautyshop:otp:resend:" + email;
    }

    private String keyAttempt(String email) {
        return "beautyshop:otp:attempt:" + email;
    }

    // Gửi mã OTP đến email
    public void guiOTP(String email) {

        String resendKey = keyResend(email);

        if (Boolean.TRUE.equals(redisTemplate.hasKey(resendKey))) {
            throw new IllegalStateException(
                    "Vui lòng đợi 60 giây trước khi gửi lại mã OTP."
            );
        }

        String otp = String.format(
                "%06d", secureRandom.nextInt(1_000_000)
        );

        // Gửi email trước khi lưu OTP
        emailService.guiMaOTP(email, otp);

        // Lưu OTP vào Redis trong 5 phút
        redisTemplate.opsForValue().set(
                keyOTP(email), otp, OTP_TTL
        );

        // Đặt thời gian chờ gửi lại
        redisTemplate.opsForValue().set(
                resendKey, "1", RESEND_TTL
        );

        // Xóa số lần nhập sai cũ
        redisTemplate.delete(keyAttempt(email));
    }

    // Xác thực mã OTP
    public boolean xacThucOTP(String email, String otpNhap) {

        String otpKey = keyOTP(email);
        String attemptKey = keyAttempt(email);

        String otpGoc = redisTemplate.opsForValue().get(otpKey);

        if (otpGoc == null || otpNhap == null) {
            return false;
        }

        Long soLan = redisTemplate.opsForValue().increment(attemptKey);

        if (soLan != null && soLan == 1) {
            redisTemplate.expire(attemptKey, ATTEMPT_TTL);
        }

        if (soLan == null || soLan > MAX_ATTEMPTS) {
            redisTemplate.delete(otpKey);
            redisTemplate.delete(attemptKey);
            return false;
        }

        boolean hopLe = MessageDigest.isEqual(
                otpGoc.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                otpNhap.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        if (hopLe) {
            redisTemplate.delete(otpKey);
            redisTemplate.delete(attemptKey);
            return true;
        }

        if (soLan >= MAX_ATTEMPTS) {
            redisTemplate.delete(otpKey);
            redisTemplate.delete(attemptKey);
        }

        return false;
    }

    // Xóa dữ liệu OTP khi cần
    public void xoaOTP(String email) {
        redisTemplate.delete(keyOTP(email));
        redisTemplate.delete(keyAttempt(email));
        redisTemplate.delete(keyResend(email));
    }
}