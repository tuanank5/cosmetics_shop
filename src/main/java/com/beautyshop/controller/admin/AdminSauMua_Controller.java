package com.beautyshop.controller.admin;

import com.beautyshop.order.ReturnStatus;
import com.beautyshop.service.SauMua_Service;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class AdminSauMua_Controller {
    private final SauMua_Service sauMua;

    AdminSauMua_Controller(SauMua_Service sauMua) { this.sauMua = sauMua; }

    @GetMapping("/admin/reviews")
    String danhGia(Model model) {
        model.addAttribute("danhGia", sauMua.danhSachDanhGiaAdmin());
        return "admin/user/reviews";
    }

    @PostMapping("/admin/reviews/{id}/visibility")
    String hienThi(@PathVariable long id, @RequestParam boolean visible,
                   @RequestParam(required = false) String revision, RedirectAttributes flash) {
        try {
            sauMua.datHienThi(id, visible, revision);
            flash.addFlashAttribute("message", visible ? "Đã duyệt đánh giá." : "Đã ẩn đánh giá.");
        } catch (IllegalStateException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/reviews";
    }

    @GetMapping("/admin/returns")
    String yeuCau(Model model) {
        model.addAttribute("yeuCau", sauMua.danhSachDoiTra(null));
        model.addAttribute("base", "/admin/returns");
        return "return/list";
    }

    @GetMapping("/admin/returns/{id}")
    String chiTiet(@PathVariable long id, Model model) {
        model.addAttribute("trang", sauMua.chiTiet(id, null));
        model.addAttribute("base", "/admin/returns");
        return "return/detail";
    }

    @PostMapping("/admin/returns/{id}/status")
    String trangThai(@PathVariable long id, @RequestParam ReturnStatus status, RedirectAttributes flash) {
        try {
            sauMua.chuyenDoiTra(id, status);
            flash.addFlashAttribute("message", "Đã cập nhật yêu cầu đổi trả.");
        } catch (IllegalStateException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/returns/" + id;
    }
}
