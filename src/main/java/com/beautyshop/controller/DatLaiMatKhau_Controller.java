package com.beautyshop.controller;

import com.beautyshop.dto.MatKhauMoi_Form;
import com.beautyshop.service.MatKhau_Service;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class DatLaiMatKhau_Controller {
    private static final String KEY = DatLaiMatKhau_Controller.class.getName() + ".token";
    private record TokenDatLai(String value, Instant expires) implements java.io.Serializable {}
    private final MatKhau_Service matKhau;

    DatLaiMatKhau_Controller(MatKhau_Service matKhau) { this.matKhau = matKhau; }

    @GetMapping("/forgot-password")
    String quen(HttpServletResponse response) {
        headers(response);
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    String yeuCau(@RequestParam(defaultValue = "") String email, HttpServletRequest request,
                 HttpServletResponse response, RedirectAttributes flash) {
        headers(response);
        matKhau.quenMatKhau(email.strip(), request.getRemoteAddr());
        flash.addFlashAttribute("message", "Nếu tài khoản đủ điều kiện, bạn sẽ nhận được email hướng dẫn.");
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    String datLai(@RequestParam(required = false) String token, HttpSession session,
                 HttpServletResponse response, Model model) {
        headers(response);
        if (token != null) {
            session.removeAttribute(KEY);
            if (matKhau.linkConHieuLuc(token)) {
                session.setAttribute(KEY, new TokenDatLai(token, Instant.now().plusSeconds(900)));
            }
            return "redirect:/reset-password";
        }
        model.addAttribute("coLink", matKhau.linkConHieuLuc(token(session)));
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    String luu(@Valid @ModelAttribute("form") MatKhauMoi_Form form, BindingResult errors,
               HttpServletRequest request, HttpServletResponse response, Authentication authentication,
               RedirectAttributes flash) {
        headers(response);
        var session = request.getSession();
        String token = token(session);
        if (token == null) {
            return "redirect:/reset-password";
        }
        if (errors.hasErrors()) {
            flash.addFlashAttribute("error", "Mật khẩu cần từ 12 ký tự, tối đa 72 byte và khớp xác nhận.");
        } else if (matKhau.datLai(token, form.getMatKhauMoi())) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            return "redirect:/login?passwordChanged";
        } else {
            session.removeAttribute(KEY);
        }
        return "redirect:/reset-password";
    }

    private static String token(HttpSession session) {
        var value = (TokenDatLai) session.getAttribute(KEY);
        return value != null && Instant.now().isBefore(value.expires()) ? value.value() : null;
    }

    private static void headers(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
    }
}
