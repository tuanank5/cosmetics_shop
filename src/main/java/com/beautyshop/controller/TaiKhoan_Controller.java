package com.beautyshop.controller;

import com.beautyshop.dto.DangKy_Form;
import com.beautyshop.dto.HoSo_Form;
import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.service.NguoiDung_Service;
import com.beautyshop.service.NguoiDung_Service.DangKyChoXacThuc;
import com.beautyshop.service.XacThucEmail_Service;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.WebUtils;

@Controller
class TaiKhoan_Controller {
    private static final String PHIEN_DANG_KY = "dangKyChoXacThuc";
    private static final String TRANG_DANG_KY = "auth/register";
    private static final String VE_DANG_KY = "redirect:/register";
    private static final String THONG_BAO_LOI = "loiDangKy";
    private static final Duration HAN_DANG_KY = Duration.ofMinutes(15);

    private final NguoiDung_Service nguoiDungService;
    private final XacThucEmail_Service xacThucEmailService;

    TaiKhoan_Controller(NguoiDung_Service nguoiDungService, XacThucEmail_Service xacThucEmailService) {
        this.nguoiDungService = nguoiDungService;
        this.xacThucEmailService = xacThucEmailService;
    }

    @GetMapping("/register")
    String dangKy(Model model, HttpSession session) {
        model.addAttribute("form", new DangKy_Form());
        synchronized (WebUtils.getSessionMutex(session)) {
            var phien = phienDangKy(session);
            if (phien != null) {
                model.addAttribute("emailChoXacThuc", phien.thongTin().email());
                model.addAttribute("daXacThuc", phien.daXacThuc());
            }
        }
        return TRANG_DANG_KY;
    }

    @PostMapping("/register")
    String dangKy(@Valid @ModelAttribute("form") DangKy_Form form, BindingResult errors,
                  HttpSession session, RedirectAttributes flash) {
        synchronized (WebUtils.getSessionMutex(session)) {
            if (phienDangKy(session) != null) {
                return VE_DANG_KY;
            }
            if (errors.hasErrors()) {
                return TRANG_DANG_KY;
            }
            try {
                String email = NguoiDung_Service.chuanHoaEmail(form.getEmail());
                if (nguoiDungService.emailDaTonTai(email)) {
                    errors.rejectValue("email", "register.duplicate", "Email đã được sử dụng.");
                    return TRANG_DANG_KY;
                }
                UUID maDangKy = UUID.randomUUID();
                var thongTin = nguoiDungService.chuanBiDangKy(form);
                if (!xacThucEmailService.guiMa(maDangKy, email)) {
                    errors.reject("register.cooldown", "Vui lòng đợi 60 giây trước khi yêu cầu mã mới.");
                    return TRANG_DANG_KY;
                }
                session.setAttribute(PHIEN_DANG_KY,
                    new PhienDangKy(maDangKy, thongTin, Instant.now().plus(HAN_DANG_KY), false));
                flash.addFlashAttribute("tinDangKy", "Đã gửi mã xác thực đến email của bạn.");
                return VE_DANG_KY;
            } catch (DataAccessException | TransactionException | MailException exception) {
                errors.reject("register.unavailable", "Chưa thể gửi mã xác thực. Vui lòng thử lại sau.");
                return TRANG_DANG_KY;
            }
        }
    }

    @PostMapping("/register/verify-otp")
    String xacThuc(@RequestParam(defaultValue = "") String otp, HttpSession session, RedirectAttributes flash) {
        synchronized (WebUtils.getSessionMutex(session)) {
            var phien = phienDangKy(session);
            if (phien == null) {
                return baoLoi(flash, "Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại.");
            }
            try {
                if (!phien.daXacThuc()) {
                    if (!otp.matches("[0-9]{6}") || !xacThucEmailService.xacThuc(phien.ma(), otp)) {
                        return baoLoi(flash, "Mã không hợp lệ, đã hết hạn hoặc đã hết lượt thử.");
                    }
                    session.setAttribute(PHIEN_DANG_KY,
                        new PhienDangKy(phien.ma(), phien.thongTin(), phien.hetHan(), true));
                }
                nguoiDungService.dangKy(phien.thongTin());
                session.removeAttribute(PHIEN_DANG_KY);
                return "redirect:/login?registered";
            } catch (DataIntegrityViolationException exception) {
                session.removeAttribute(PHIEN_DANG_KY);
                return baoLoi(flash, "Email đã được sử dụng. Vui lòng đăng nhập hoặc dùng email khác.");
            } catch (DataAccessException | TransactionException exception) {
                return baoLoi(flash, "Chưa thể hoàn tất đăng ký. Vui lòng thử lại.");
            }
        }
    }

    @PostMapping("/register/resend-otp")
    String guiLai(HttpSession session, RedirectAttributes flash) {
        synchronized (WebUtils.getSessionMutex(session)) {
            var phien = phienDangKy(session);
            if (phien == null) {
                return baoLoi(flash, "Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại.");
            }
            if (phien.daXacThuc()) {
                return VE_DANG_KY;
            }
            try {
                if (!xacThucEmailService.guiMa(phien.ma(), phien.thongTin().email())) {
                    return baoLoi(flash, "Vui lòng đợi 60 giây trước khi yêu cầu mã mới.");
                }
                flash.addFlashAttribute("tinDangKy", "Đã gửi mã xác thực mới.");
                return VE_DANG_KY;
            } catch (DataAccessException | MailException exception) {
                return baoLoi(flash, "Chưa thể gửi lại mã xác thực. Vui lòng thử lại sau.");
            }
        }
    }

    @PostMapping("/register/cancel")
    String huyDangKy(HttpSession session) {
        synchronized (WebUtils.getSessionMutex(session)) {
            session.removeAttribute(PHIEN_DANG_KY);
        }
        return VE_DANG_KY;
    }

    private static PhienDangKy phienDangKy(HttpSession session) {
        var phien = (PhienDangKy) session.getAttribute(PHIEN_DANG_KY);
        if (phien != null && !Instant.now().isBefore(phien.hetHan())) {
            session.removeAttribute(PHIEN_DANG_KY);
            return null;
        }
        return phien;
    }

    private static String baoLoi(RedirectAttributes flash, String message) {
        flash.addFlashAttribute(THONG_BAO_LOI, message);
        return VE_DANG_KY;
    }

    private record PhienDangKy(UUID ma, DangKyChoXacThuc thongTin, Instant hetHan, boolean daXacThuc) {}

    @GetMapping("/account")
    String taiKhoan(@AuthenticationPrincipal PhienNguoiDung nguoiDung, Model model) {
        model.addAttribute("email", nguoiDung.getUsername());
        model.addAttribute("hoSo", nguoiDungService.hoSo(nguoiDung.getMaNguoiDung()));
        return "auth/account";
    }

    @PostMapping("/account")
    String capNhatHoSo(@AuthenticationPrincipal PhienNguoiDung nguoiDung,
                     @Valid @ModelAttribute("hoSo") HoSo_Form form, BindingResult errors,
                     Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            nguoiDungService.capNhatHoSo(nguoiDung.getMaNguoiDung(), form);
            flash.addFlashAttribute("message", "Đã cập nhật hồ sơ.");
            return "redirect:/account";
        }
        model.addAttribute("email", nguoiDung.getUsername());
        return "auth/account";
    }
}
