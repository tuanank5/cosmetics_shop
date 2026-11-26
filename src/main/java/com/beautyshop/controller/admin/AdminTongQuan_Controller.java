package com.beautyshop.controller.admin;

import com.beautyshop.dto.BoLocThongKe_Form;
import com.beautyshop.repository.ThongKe_Repository;
import com.beautyshop.service.ThongKe_Service;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
class AdminTongQuan_Controller {
    private final ThongKe_Service thongKeService;

    AdminTongQuan_Controller(ThongKe_Service thongKeService) { this.thongKeService = thongKeService; }

    @GetMapping({"/admin", "/admin/"})
    String trangQuanTri() { return "redirect:/admin/dashboard"; }

    @GetMapping("/admin/dashboard")
    String dashboard(@ModelAttribute("boLoc") BoLocThongKe_Form boLoc, BindingResult errors,
                     @RequestParam(name = "nguong", required = false) String nguong, Model model) {
        var ky = thongKeService.xacDinhKy(boLoc, errors);
        int nguongTon = docNguong(nguong, errors);
        model.addAttribute("ky", ky);
        model.addAttribute("homNay", thongKeService.homNay());
        model.addAttribute("tongQuan", thongKeService.tongQuan(ky, nguongTon));
        model.addAttribute("trangThaiChoXuLy", ThongKe_Repository.TRANG_THAI_CHO_XU_LY);
        return "admin/dashboard";
    }

    private static int docNguong(String nguong, BindingResult errors) {
        if (nguong == null || nguong.isBlank()) {
            return ThongKe_Service.NGUONG_TON_MAC_DINH;
        }
        try {
            int giaTri = Integer.parseInt(nguong.strip());
            if (giaTri >= 0 && giaTri <= 1000) {
                return giaTri;
            }
        } catch (NumberFormatException ignored) {
            // rơi xuống thông báo lỗi bên dưới
        }
        errors.reject("nguong", "Số lượng để báo sắp hết hàng phải là số nguyên từ 0 đến 1000. Đang dùng mức 5.");
        return ThongKe_Service.NGUONG_TON_MAC_DINH;
    }
}
