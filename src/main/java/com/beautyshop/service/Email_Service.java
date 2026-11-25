
package com.beautyshop.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class Email_Service {

    private final JavaMailSender mailSender;

    public Email_Service(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void guiMaOTP(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("BeautyShop - Xác thực email đăng ký");

        message.setText(
                "Xin chào,\n\n" +
                        "Cảm ơn bạn đã đăng ký tài khoản BeautyShop.\n\n" +
                        "Mã xác thực OTP của bạn là: " + otp + "\n\n" +
                        "Mã OTP có hiệu lực trong 5 phút.\n" +
                        "Vui lòng không chia sẻ mã này với bất kỳ ai.\n\n" +
                        "Trân trọng,\nBeautyShop"
        );

        mailSender.send(message);
    }
}