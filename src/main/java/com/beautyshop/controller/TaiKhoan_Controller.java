
package com.beautyshop.controller;

import com.beautyshop.dto.DangKy_Form;
import com.beautyshop.dto.HoSo_Form;
import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.service.EmailVerification_Service;
import com.beautyshop.service.NguoiDung_Service;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.WebUtils;

@Controller
class TaiKhoan_Controller {

    private final NguoiDung_Service nguoiDungService;
    private final EmailVerification_Service emailVerificationService;
    private static final Logger logger = LoggerFactory.getLogger(TaiKhoan_Controller.class);

    private static final String SESSION_EMAIL = "registerEmail";
    private static final String SESSION_NAME = "registerHoTen";
    private static final String SESSION_CHALLENGE = "registerChallenge";
    private static final String SESSION_PASSWORD = "registerPasswordHash";

    TaiKhoan_Controller(
            NguoiDung_Service nguoiDungService,
            EmailVerification_Service emailVerificationService) {

        this.nguoiDungService = nguoiDungService;
        this.emailVerificationService = emailVerificationService;
    }

    // Hiển thị trang đăng ký
    @GetMapping("/register")
    String dangKy(Model model, HttpSession session) {
        model.addAttribute("form", new DangKy_Form());
        model.addAttribute("otpSent", session.getAttribute(SESSION_EMAIL) != null
        );
        return "auth/register";
    }

    // Bước 1: Kiểm tra thông tin và gửi OTP
    @PostMapping("/register")
    String dangKy(@Valid @ModelAttribute("form") DangKy_Form form, BindingResult errors, Model model, HttpSession session, RedirectAttributes flash) {
        synchronized (WebUtils.getSessionMutex(session)) {
            return batDauDangKy(form, errors, session, flash);
        }
    }

    private String batDauDangKy(DangKy_Form form, BindingResult errors, HttpSession session, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            return "auth/register";
        }
        String email = NguoiDung_Service.chuanHoaEmail(form.getEmail());
        if (nguoiDungService.emailDaTonTai(email)) {
            errors.rejectValue("email","register.duplicate","Email đã được sử dụng."
            );
            return "auth/register";
        }

        try {
            // Mã hóa mật khẩu trước khi lưu tạm
            String matKhauBam = nguoiDungService.bamMatKhau(form.getMatKhau());
            // Gửi OTP đến email
            String challenge = emailVerificationService.guiOTP(email, "register");
            emailVerificationService.xoaOTP((String) session.getAttribute(SESSION_CHALLENGE));
            session.setAttribute(SESSION_CHALLENGE, challenge);
            // Chỉ lưu thông tin đã mã hóa trong Session
            session.setAttribute(SESSION_EMAIL, email);
            session.setAttribute(SESSION_NAME, form.getHoTen().strip());
            session.setAttribute(SESSION_PASSWORD, matKhauBam);
            flash.addFlashAttribute("message", "Mã OTP đã được gửi đến email của bạn.");
            return "redirect:/register";
        } catch (IllegalStateException exception) {
            logger.warn("Đăng ký chưa thể gửi OTP: {}", exception.getClass().getSimpleName());
            errors.reject("register.otp", "Chưa thể gửi OTP. Vui lòng đợi rồi thử lại.");
        } catch (RuntimeException exception) {
            logger.warn("Đăng ký chưa thể gửi OTP: {}", exception.getClass().getSimpleName());
            errors.reject("register.mail", "Không thể gửi mã OTP. Vui lòng kiểm tra cấu hình email và thử lại.");
        }
        return "auth/register";
    }

    // Bước 2: Xác thực OTP và tạo tài khoản
    @PostMapping("/register/verify-otp")
    String xacThucOTP(@RequestParam("otp") String otp, HttpSession session, RedirectAttributes flash) {
        synchronized (WebUtils.getSessionMutex(session)) {
            return hoanTatDangKy(otp, session, flash);
        }
    }

    private String hoanTatDangKy(String otp, HttpSession session, RedirectAttributes flash) {
        String email = (String) session.getAttribute(SESSION_EMAIL);
        String hoTen = (String) session.getAttribute(SESSION_NAME);
        String matKhauBam = (String) session.getAttribute(SESSION_PASSWORD);
        if (email == null || hoTen == null || matKhauBam == null) {
            flash.addFlashAttribute("error", "Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại.");
            return "redirect:/register";
        }
        if (otp == null || !otp.matches("\\d{6}")) {
            flash.addFlashAttribute("error", "Mã OTP phải gồm đúng 6 chữ số.");
            return "redirect:/register";
        }

        try {
            boolean hopLe = emailVerificationService.xacThucOTP((String) session.getAttribute(SESSION_CHALLENGE), otp);
            if (!hopLe) {flash.addFlashAttribute("error", "Mã OTP không chính xác, đã hết hạn hoặc vượt quá số lần thử.");
                return "redirect:/register";
            }

            // Chỉ tạo tài khoản khi OTP hợp lệ
            nguoiDungService.dangKyDaXacThuc(email, hoTen, matKhauBam);
            session.removeAttribute(SESSION_CHALLENGE);
            session.removeAttribute(SESSION_EMAIL);
            session.removeAttribute(SESSION_NAME);
            session.removeAttribute(SESSION_PASSWORD);
            flash.addFlashAttribute("message", "Đăng ký thành công! Bạn có thể đăng nhập."
            );
            return "redirect:/login?registered";
        } catch (DataIntegrityViolationException exception) {
            session.removeAttribute(SESSION_CHALLENGE);
            session.removeAttribute(SESSION_EMAIL);
            session.removeAttribute(SESSION_NAME);
            session.removeAttribute(SESSION_PASSWORD);
            flash.addFlashAttribute("error", "Email đã được sử dụng. Vui lòng đăng ký bằng email khác.");
            return "redirect:/register";
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", "Không thể hoàn tất đăng ký. Vui lòng thử lại.");
            return "redirect:/register";
        }
    }

    // Gửi lại OTP
    @PostMapping("/register/resend-otp")
    String guiLaiOTP(
            HttpSession session,
            RedirectAttributes flash) {
        synchronized (WebUtils.getSessionMutex(session)) {
            return capLaiMa(session, flash);
        }
    }

    private String capLaiMa(HttpSession session, RedirectAttributes flash) {
        String email = (String) session.getAttribute(SESSION_EMAIL);

        if (email == null) {
            flash.addFlashAttribute(
                    "error",
                    "Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại."
            );
            return "redirect:/register";
        }

        try {
            String challenge = emailVerificationService.guiOTP(email, "register");
            emailVerificationService.xoaOTP((String) session.getAttribute(SESSION_CHALLENGE));
            session.setAttribute(SESSION_CHALLENGE, challenge);

            flash.addFlashAttribute(
                    "message",
                    "Mã OTP mới đã được gửi đến email của bạn."
            );

        } catch (IllegalStateException exception) {

            flash.addFlashAttribute(
                    "error",
                    "Chưa thể gửi lại OTP. Vui lòng đợi rồi thử lại."
            );

        } catch (RuntimeException exception) {
            logger.warn("Đăng ký chưa thể gửi lại OTP: {}", exception.getClass().getSimpleName());
            flash.addFlashAttribute(
                    "error",
                    "Không thể gửi lại OTP. Vui lòng thử lại sau."
            );
        }

        return "redirect:/register";
    }

    // Hủy đăng ký đang chờ xác thực
    @PostMapping("/register/cancel")
    String huyDangKy(HttpSession session, RedirectAttributes flash) {
        synchronized (WebUtils.getSessionMutex(session)) {
            session.removeAttribute(SESSION_CHALLENGE);
            session.removeAttribute(SESSION_EMAIL);
            session.removeAttribute(SESSION_NAME);
            session.removeAttribute(SESSION_PASSWORD);
        }

        flash.addFlashAttribute(
                "message",
                "Đã hủy quá trình đăng ký."
        );

        return "redirect:/register";
    }

    // Giữ nguyên chức năng xem tài khoản
    @GetMapping("/account")
    String taiKhoan(
            @AuthenticationPrincipal PhienNguoiDung nguoiDung,
            Model model) {

        model.addAttribute("email", nguoiDung.getUsername());

        model.addAttribute(
                "hoSo",
                nguoiDungService.hoSo(nguoiDung.getMaNguoiDung())
        );

        return "auth/account";
    }

    // Giữ nguyên chức năng cập nhật hồ sơ
    @PostMapping("/account")
    String capNhatHoSo(
            @AuthenticationPrincipal PhienNguoiDung nguoiDung,
            @Valid @ModelAttribute("hoSo") HoSo_Form form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {

        if (!errors.hasErrors()) {

            nguoiDungService.capNhatHoSo(
                    nguoiDung.getMaNguoiDung(),
                    form
            );

            flash.addFlashAttribute(
                    "message",
                    "Đã cập nhật hồ sơ."
            );

            return "redirect:/account";
        }

        model.addAttribute("email", nguoiDung.getUsername());

        return "auth/account";
    }
}
