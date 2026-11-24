package com.beautyshop.controller;

import com.beautyshop.dto.DangKy_Form;
import com.beautyshop.dto.HoSo_Form;
import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.service.NguoiDung_Service;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class TaiKhoan_Controller {
    private final NguoiDung_Service nguoiDungService;

    TaiKhoan_Controller(NguoiDung_Service nguoiDungService) {
        this.nguoiDungService = nguoiDungService;
    }

    @GetMapping("/register")
    String dangKy(Model model) {
        model.addAttribute("form", new DangKy_Form());
        return "auth/register";
    }

    @PostMapping("/register")
    String dangKy(@Valid @ModelAttribute("form") DangKy_Form form, BindingResult errors) {
        if (!errors.hasErrors()) {
            try {
                nguoiDungService.dangKy(form);
                return "redirect:/login?registered";
            } catch (DataIntegrityViolationException exception) {
                errors.rejectValue("email", "register.duplicate", "Email đã được sử dụng.");
            }
        }
        return "auth/register";
    }

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
