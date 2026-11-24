package com.beautyshop.controller;

import com.beautyshop.dto.DiaChiNguoiDung_Form;
import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.service.DiaChiNguoiDung_Service;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account/addresses")
class DiaChiNguoiDung_Controller {
    private final DiaChiNguoiDung_Service diaChiService;

    DiaChiNguoiDung_Controller(DiaChiNguoiDung_Service diaChiService) {
        this.diaChiService = diaChiService;
    }

    @GetMapping
    String danhSach(@AuthenticationPrincipal PhienNguoiDung nguoiDung, Model model) {
        model.addAttribute("diaChi", diaChiService.danhSach(nguoiDung.getMaNguoiDung()));
        return "auth/addresses";
    }

    @GetMapping({"/new", "/{id}/edit"})
    String form(@PathVariable(required = false) Long id, @AuthenticationPrincipal PhienNguoiDung nguoiDung, Model model) {
        model.addAttribute("form", id == null ? new DiaChiNguoiDung_Form() : diaChiService.form(nguoiDung.getMaNguoiDung(), id));
        return hienForm(id, model);
    }

    @PostMapping({"/new", "/{id}/edit"})
    String luu(@PathVariable(required = false) Long id, @AuthenticationPrincipal PhienNguoiDung nguoiDung,
               @Valid @ModelAttribute("form") DiaChiNguoiDung_Form form, BindingResult errors,
               Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            if (id != null) {
                diaChiService.form(nguoiDung.getMaNguoiDung(), id);
            }
            return hienForm(id, model);
        }
        diaChiService.luu(nguoiDung.getMaNguoiDung(), id, form);
        flash.addFlashAttribute("message", "Đã lưu địa chỉ.");
        return "redirect:/account/addresses";
    }

    @PostMapping("/{id}/delete")
    String xoa(@PathVariable long id, @AuthenticationPrincipal PhienNguoiDung nguoiDung, RedirectAttributes flash) {
        diaChiService.xoa(nguoiDung.getMaNguoiDung(), id);
        flash.addFlashAttribute("message", "Đã xóa địa chỉ.");
        return "redirect:/account/addresses";
    }

    private String hienForm(Long id, Model model) {
        model.addAttribute("action", "/account/addresses/" + (id == null ? "new" : id + "/edit"));
        return "auth/address-form";
    }
}
