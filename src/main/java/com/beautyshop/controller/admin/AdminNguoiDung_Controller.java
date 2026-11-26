package com.beautyshop.controller.admin;

import com.beautyshop.controller.NguonDieuHuong;
import com.beautyshop.dto.PhanTrang;
import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.service.NguoiDung_Service;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
class AdminNguoiDung_Controller {
    private final NguoiDung_Service nguoiDungService;

    AdminNguoiDung_Controller(NguoiDung_Service nguoiDungService) {
        this.nguoiDungService = nguoiDungService;
    }

    @GetMapping
    String danhSach(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page,
                    @AuthenticationPrincipal PhienNguoiDung nguoiDung, Model model) {
        String timKiem = chuanHoa(q);
        var pageable = PageRequest.of(Math.clamp(page, 0, 100_000), 15);
        var ketQua = PhanTrang.doc(pageable, trang -> nguoiDungService.timNguoiDung(timKiem, trang));
        model.addAttribute("pager", PhanTrang.tu(ketQua,
            so -> NguonDieuHuong.trangDanhSach("/admin/users", Map.of("q", timKiem), so)));
        model.addAttribute("q", timKiem);
        model.addAttribute("ketQua", ketQua);
        model.addAttribute("maAdmin", nguoiDung.getMaNguoiDung());
        return "admin/user/list";
    }

    @PostMapping("/{id}/status")
    String trangThai(@PathVariable long id, @RequestParam boolean enabled,
                     @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page,
                     @AuthenticationPrincipal PhienNguoiDung nguoiDung, RedirectAttributes flash) {
        if (nguoiDungService.datTrangThai(nguoiDung.getMaNguoiDung(), nguoiDung.getPhienBanXacThuc(), id, enabled)) {
            flash.addFlashAttribute("message", enabled ? "Đã mở tài khoản." : "Đã khóa tài khoản.");
        } else {
            flash.addFlashAttribute("error", "Bạn không thể khóa tài khoản đang sử dụng.");
        }
        flash.addAttribute("q", chuanHoa(q));
        flash.addAttribute("page", Math.max(0, page));
        return "redirect:/admin/users";
    }

    private String chuanHoa(String q) {
        String timKiem = q.strip();
        int doDai = timKiem.codePointCount(0, timKiem.length());
        return doDai <= 190 ? timKiem : timKiem.substring(0, timKiem.offsetByCodePoints(0, 190));
    }
}
