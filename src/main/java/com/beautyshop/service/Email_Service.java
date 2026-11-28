
package com.beautyshop.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
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

    private static final String KHOA = """
        Xin chào bạn,

        BeautyShop đã khóa tài khoản của bạn lúc %s (giờ Việt Nam). Trong thời gian tài khoản bị khóa, bạn sẽ không thể đăng nhập vào BeautyShop.

        Nếu bạn không nhận ra thay đổi này hoặc muốn yêu cầu xem xét mở khóa, vui lòng vào trang chủ BeautyShop và liên hệ bộ phận hỗ trợ qua thông tin ở cuối trang. Bạn có thể cung cấp thời điểm ghi trong thư để chúng tôi tra cứu. Vui lòng giữ kín mật khẩu và mã xác minh của bạn.

        Trân trọng,
        BeautyShop""";
    private static final String MO = """
        Xin chào bạn,

        BeautyShop đã mở khóa tài khoản của bạn lúc %s (giờ Việt Nam). Bạn có thể đăng nhập lại trên BeautyShop bằng phương thức đã thiết lập cho tài khoản.

        Nếu bạn vẫn gặp khó khăn hoặc không nhận ra thay đổi này, vui lòng vào trang chủ BeautyShop và liên hệ bộ phận hỗ trợ qua thông tin ở cuối trang. Bạn có thể cung cấp thời điểm ghi trong thư để chúng tôi tra cứu. Vui lòng giữ kín mật khẩu và mã xác minh của bạn.

        Trân trọng,
        BeautyShop""";

    void guiTrangThai(UUID event, String email, boolean moKhoa, LocalDateTime utc) {
        var gio = utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh"));
        var luc = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(gio);
        mailSender.send(message -> {
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            if (!from.isBlank()) helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject(moKhoa ? "BeautyShop đã mở khóa tài khoản của bạn" : "BeautyShop đã khóa tài khoản của bạn");
            helper.setText((moKhoa ? MO : KHOA).formatted(luc));
            message.setHeader("Message-ID", "<" + event + "@account.beautyshop.invalid>");
        });
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
