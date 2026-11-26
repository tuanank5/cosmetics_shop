
package com.beautyshop.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class Email_Service {

    private final JavaMailSender mailSender;
    private final String from;

    public Email_Service(JavaMailSender mailSender, @Value("${MAIL_FROM:${MAIL_USERNAME:}}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void guiMaOTP(String email, String otp) {
        gui(email, "BeautyShop - Xác thực email đăng ký",
                "Xin chào,\n\n" +
                        "Cảm ơn bạn đã đăng ký tài khoản BeautyShop.\n\n" +
                        "Mã xác thực OTP của bạn là: " + otp + "\n\n" +
                        "Mã OTP có hiệu lực trong 5 phút.\n" +
                        "Vui lòng không chia sẻ mã này với bất kỳ ai.\n\n" +
                        "Trân trọng,\nBeautyShop"
        );

    }

    public void guiMaBaoMat(String email, String otp) {
        gui(email, "Mã xác minh bảo mật BeautyShop",
            "Mã của bạn là " + otp + ". Mã có hiệu lực 5 phút. Vui lòng giữ mã riêng tư.");
    }

    public void guiLinkDatLai(String email, String link) {
        gui(email, "Đặt lại mật khẩu BeautyShop",
            "Mở đường dẫn trong vòng 15 phút để tự đặt mật khẩu mới:\n" + link
                + "\nNếu bạn không yêu cầu, bạn có thể bỏ qua thư này.");
    }

    public void baoDaDoiMatKhau(String email) {
        gui(email, "Mật khẩu BeautyShop đã thay đổi",
            "Mật khẩu vừa được cập nhật. Nếu bạn không thực hiện, hãy liên hệ hỗ trợ BeautyShop.");
    }

    private void gui(String email, String subject, String body) {
        var message = new SimpleMailMessage();
        if (!from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
