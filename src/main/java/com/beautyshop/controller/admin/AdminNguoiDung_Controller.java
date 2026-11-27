package com.beautyshop.controller.admin;

import com.beautyshop.controller.NguonDieuHuong;
import com.beautyshop.dto.PhanTrang;
import com.beautyshop.model.NguoiDung.VaiTro;
import java.util.LinkedHashMap;
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
    String danhSach(@RequestParam(defaultValue = "") String q, @RequestParam(required = false) Boolean active,
                    @RequestParam(required = false) VaiTro role,
                    @RequestParam(defaultValue = "0") int page,
                    @AuthenticationPrincipal PhienNguoiDung nguoiDung, Model model) {
        String timKiem = chuanHoa(q);
        var pageable = PageRequest.of(Math.clamp(page, 0, 100_000), 15);
        var ketQua = PhanTrang.doc(pageable, trang -> nguoiDungService.timNguoiDung(timKiem, active, role, trang));
        var boLoc = boLoc(timKiem, active, role);
        model.addAttribute("userListQuery", NguonDieuHuong.trangDanhSach("", boLoc, ketQua.getNumber()) + "&fromList=true");
        model.addAttribute("pager", PhanTrang.tu(ketQua,
            so -> NguonDieuHuong.trangDanhSach("/admin/users", boLoc, so)));
        model.addAttribute("q", timKiem);
        model.addAttribute("active", active);
        model.addAttribute("role", role);
        model.addAttribute("ketQua", ketQua);
        model.addAttribute("maAdmin", nguoiDung.getMaNguoiDung());
        return "admin/user/list";
    }

    @PostMapping("/{id}/status")
    String trangThai(@PathVariable long id, @RequestParam boolean enabled,
                     @RequestParam(defaultValue = "") String q, @RequestParam(required = false) Boolean active,
                    @RequestParam(required = false) VaiTro role,
                     @RequestParam(defaultValue = "0") int page,
                     @RequestParam(defaultValue = "false") boolean fromList,
                     @AuthenticationPrincipal PhienNguoiDung nguoiDung, RedirectAttributes flash) {
        if (nguoiDungService.datTrangThai(nguoiDung.getMaNguoiDung(), nguoiDung.getPhienBanXacThuc(), id, enabled)) {
            flash.addFlashAttribute("message", enabled ? "Đã mở tài khoản." : "Đã khóa tài khoản.");
        } else {
            flash.addFlashAttribute("error", "Bạn không thể khóa tài khoản đang sử dụng.");
        }
        flash.addAttribute("q", chuanHoa(q));
        if (active != null) flash.addAttribute("active", active);
        if (role != null) flash.addAttribute("role", role);
        flash.addAttribute("page", Math.max(0, page));
        return "redirect:/admin/users" + (fromList ? NguonDieuHuong.mocAdmin(id) : "");
    }

    static Map<String, String> boLoc(String q, Boolean active, VaiTro role) {
        var thamSo = new LinkedHashMap<>(Map.of("q", q));
        if (active != null) thamSo.put("active", active.toString());
        if (role != null) thamSo.put("role", role.name());
        return thamSo;
    }

    private String chuanHoa(String q) {
        String timKiem = q.strip();
        int doDai = timKiem.codePointCount(0, timKiem.length());
        return doDai <= 190 ? timKiem : timKiem.substring(0, timKiem.offsetByCodePoints(0, 190));
    }
}
