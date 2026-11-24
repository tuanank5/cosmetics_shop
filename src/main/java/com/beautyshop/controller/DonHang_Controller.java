package com.beautyshop.controller;

import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.service.DonHang_Service;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/orders")
class DonHang_Controller {
    private final DonHang_Service donHangService;

    DonHang_Controller(DonHang_Service donHangService) { this.donHangService = donHangService; }

    @GetMapping
    String danhSach(@AuthenticationPrincipal PhienNguoiDung phien, Model model) {
        model.addAttribute("donHang", donHangService.danhSachCustomer(phien.getMaNguoiDung()));
        model.addAttribute("base", "/orders");
        return "order/list";
    }

    @GetMapping("/{id}")
    String chiTiet(@PathVariable long id, Model model) {
        model.addAttribute("chiTiet", donHangService.chiTietCustomer(id));
        model.addAttribute("base", "/orders");
        return "order/detail";
    }

    @PostMapping("/{id}/cancel")
    String huy(@PathVariable long id, RedirectAttributes flash) {
        try {
            donHangService.huyCustomer(id);
            flash.addFlashAttribute("message", "Đơn đã hủy.");
        } catch (IllegalStateException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/orders/" + id;
    }
}
