package com.beautyshop.controller.admin;

import com.beautyshop.controller.NguonDieuHuong;
import com.beautyshop.dto.PhanTrang;
import com.beautyshop.model.NguoiDung.VaiTro;
import java.util.LinkedHashMap;
import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.service.NguoiDung_Service;
import java.util.Map;
import java.util.UUID;
import com.beautyshop.service.LyDoTaiKhoan;
import com.beautyshop.service.ThuTrangThaiTaiKhoan;
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

    private final PhienDoiTrangThai form;
    private final ThuTrangThaiTaiKhoan thu;

    AdminNguoiDung_Controller(NguoiDung_Service nguoiDungService, PhienDoiTrangThai form, ThuTrangThaiTaiKhoan thu) {
        this.form = form;
        this.thu = thu;
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
        var lenh = new LinkedHashMap<Long, UUID>();
        for (var muc : ketQua) {
            lenh.put(muc.getMaNguoiDung(), form.cap(nguoiDung.getMaNguoiDung(), muc.getMaNguoiDung(),
                muc.getPhienBanXacThuc(), !muc.isKichHoat()));
        }
        model.addAttribute("lenh", lenh);
        model.addAttribute("thu", thu.moiNhat(lenh.keySet()));
        model.addAttribute("cacLyDo", LyDoTaiKhoan.values());
        model.addAttribute("ketQua", ketQua);
        model.addAttribute("maAdmin", nguoiDung.getMaNguoiDung());
        return "admin/user/list";
    }

    @PostMapping("/{id}/status")
    String trangThai(@PathVariable long id, @RequestParam UUID operation, @RequestParam LyDoTaiKhoan reason,
                     @RequestParam(defaultValue = "") String q, @RequestParam(required = false) Boolean active,
                    @RequestParam(required = false) VaiTro role,
                     @RequestParam(defaultValue = "0") int page,
                     @RequestParam(defaultValue = "false") boolean fromList,
                     @AuthenticationPrincipal PhienNguoiDung nguoiDung, RedirectAttributes flash) {
        var lenh = form.doc(operation, nguoiDung.getMaNguoiDung(), id);
        switch (nguoiDungService.datTrangThai(id, lenh.moKhoa(), lenh.version(), operation, reason)) {
            case DA_DOI -> flash.addFlashAttribute("message", lenh.moKhoa() ? "Đã mở tài khoản." : "Đã khóa tài khoản.");
            case DA_GHI -> flash.addFlashAttribute("message", "Thao tác này đã được ghi nhận.");
            case KHONG_DOI -> flash.addFlashAttribute("error", "Tài khoản đã ở trạng thái yêu cầu.");
            case FORM_CU -> flash.addFlashAttribute("error", "Thông tin đã thay đổi. Hãy kiểm tra lại trước khi xác nhận.");
            case TU_KHOA -> flash.addFlashAttribute("error", "Bạn không thể khóa tài khoản đang sử dụng.");
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
