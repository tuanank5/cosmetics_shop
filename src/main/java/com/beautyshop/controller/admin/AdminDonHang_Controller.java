package com.beautyshop.controller.admin;

import com.beautyshop.order.OrderStatus;
import com.beautyshop.service.DonHang_Service;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
class AdminDonHang_Controller {
    private final DonHang_Service donHangService;

    AdminDonHang_Controller(DonHang_Service donHangService) { this.donHangService = donHangService; }

    @GetMapping
    String danhSach(@RequestParam(name = "status", required = false) List<OrderStatus> loc, Model model) {
        // P20: Dashboard links here with ?status=... to show only orders waiting for Admin action.
        var donHang = donHangService.danhSachAdmin();
        if (loc != null && !loc.isEmpty()) {
            donHang = donHang.stream().filter(don -> loc.contains(don.trangThai())).toList();
            model.addAttribute("locTrangThai", loc);
        }
        model.addAttribute("donHang", donHang);
        model.addAttribute("tongQuan", donHangService.tongQuanAdmin());
        model.addAttribute("base", "/admin/orders");
        return "order/list";
    }

    @GetMapping("/{id}")
    String chiTiet(@PathVariable long id, Model model) {
        model.addAttribute("chiTiet", donHangService.chiTietAdmin(id));
        model.addAttribute("base", "/admin/orders");
        return "order/detail";
    }

    @PostMapping("/{id}/status")
    String chuyen(@PathVariable long id, @RequestParam OrderStatus status, RedirectAttributes flash) {
        try {
            donHangService.chuyenAdmin(id, status);
            flash.addFlashAttribute("message", "Đã cập nhật trạng thái đơn.");
        } catch (IllegalStateException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }
}
