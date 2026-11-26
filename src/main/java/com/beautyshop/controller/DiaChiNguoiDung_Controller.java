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
    String danhSach(@AuthenticationPrincipal PhienNguoiDung nguoiDung,
                   @RequestParam(name = "from", required = false) String nguon, Model model) {
        model.addAttribute("diaChi", diaChiService.danhSach(nguoiDung.getMaNguoiDung()));
        model.addAttribute("tuCheckout", NguonDieuHuong.tuCheckout(nguon));
        return "auth/addresses";
    }

    @GetMapping({"/new", "/{id}/edit"})
    String form(@PathVariable(required = false) Long id, @AuthenticationPrincipal PhienNguoiDung nguoiDung,
                @RequestParam(name = "from", required = false) String nguon, Model model) {
        model.addAttribute("form", id == null ? new DiaChiNguoiDung_Form() : diaChiService.form(nguoiDung.getMaNguoiDung(), id));
        return hienForm(id, nguon, model);
    }

    @PostMapping({"/new", "/{id}/edit"})
    String luu(@PathVariable(required = false) Long id, @AuthenticationPrincipal PhienNguoiDung nguoiDung,
               @RequestParam(name = "from", required = false) String nguon,
               @Valid @ModelAttribute("form") DiaChiNguoiDung_Form form, BindingResult errors,
               Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            if (id != null) {
                diaChiService.form(nguoiDung.getMaNguoiDung(), id);
            }
            return hienForm(id, nguon, model);
        }
        diaChiService.luu(nguoiDung.getMaNguoiDung(), id, form);
        flash.addFlashAttribute("message", "Đã lưu địa chỉ.");
        return "redirect:" + NguonDieuHuong.veDiaChi(nguon);
    }

    @PostMapping("/{id}/delete")
    String xoa(@PathVariable long id, @AuthenticationPrincipal PhienNguoiDung nguoiDung,
               @RequestParam(name = "from", required = false) String nguon, RedirectAttributes flash) {
        diaChiService.xoa(nguoiDung.getMaNguoiDung(), id);
        flash.addFlashAttribute("message", "Đã xóa địa chỉ.");
        if (NguonDieuHuong.tuCheckout(nguon)) flash.addAttribute("from", "checkout");
        return "redirect:/account/addresses";
    }

    private String hienForm(Long id, String nguon, Model model) {
        model.addAttribute("action", "/account/addresses/" + (id == null ? "new" : id + "/edit"));
        model.addAttribute("tuCheckout", NguonDieuHuong.tuCheckout(nguon));
        model.addAttribute("addressBack", NguonDieuHuong.veDiaChi(nguon));
        return "auth/address-form";
    }
}
