package com.beautyshop.controller.admin;

import com.beautyshop.repository.DanhTinhOAuth_Repository;
import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.service.MatKhau_Service;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users/{id}")
class AdminBaoMatTaiKhoan_Controller {
    private final NguoiDung_Repository users;
    private final DanhTinhOAuth_Repository identities;
    private final MatKhau_Service passwords;

    AdminBaoMatTaiKhoan_Controller(NguoiDung_Repository users, DanhTinhOAuth_Repository identities,
                                   MatKhau_Service passwords) {
        this.users = users;
        this.identities = identities;
        this.passwords = passwords;
    }

    @GetMapping
    String xem(@PathVariable long id, @RequestParam(defaultValue = "") String q,
               @RequestParam(defaultValue = "0") int page, Model model) {
        var user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("id", id);
        model.addAttribute("hoTen", user.getHoTen());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("coMatKhau", user.getMatKhauBam() != null);
        model.addAttribute("daXacMinhEmail", user.getNgayXacMinhEmail() != null);
        model.addAttribute("duDieuKien", MatKhau_Service.duDieuKien(user));
        model.addAttribute("providers", identities.findByNguoiDung_MaNguoiDung(id).stream()
            .map(com.beautyshop.entity.DanhTinhOAuth::getNhaCungCap).toList());
        model.addAttribute("q", timKiem(q));
        model.addAttribute("page", Math.clamp(page, 0, 100_000));
        return "admin/user/detail";
    }

    @PostMapping("/password-reset")
    String yeuCau(@PathVariable long id, @RequestParam MatKhau_Service.LyDoHoTro reason,
                  @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page,
                  @AuthenticationPrincipal PhienNguoiDung admin, RedirectAttributes flash) {
        switch (passwords.adminYeuCau(admin.getMaNguoiDung(), admin.getPhienBanXacThuc(), id, reason)) {
            case DA_GUI -> flash.addFlashAttribute("message", "Máy chủ thư đã nhận yêu cầu gửi liên kết.");
            case KHONG_DU_DIEU_KIEN -> flash.addFlashAttribute("error",
                "Chưa thể cấp liên kết cho tài khoản này. Kiểm tra điều kiện hoặc thử lại sau.");
            case LOI_GUI -> flash.addFlashAttribute("error", "Chưa gửi được thư. Vui lòng thử lại sau.");
        }
        flash.addAttribute("q", timKiem(q));
        flash.addAttribute("page", Math.clamp(page, 0, 100_000));
        return "redirect:/admin/users/" + id;
    }

    private static String timKiem(String q) {
        return q.codePointCount(0, q.length()) <= 190 ? q : q.substring(0, q.offsetByCodePoints(0, 190));
    }
}
