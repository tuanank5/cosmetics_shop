package com.beautyshop.controller.admin;

import com.beautyshop.controller.NguonDieuHuong;
import com.beautyshop.controller.NguonDieuHuong.NguonDonAdmin;
import com.beautyshop.dto.BoLocDonHangAdmin;
import com.beautyshop.dto.PhanTrang;
import com.beautyshop.order.OrderStatus;
import com.beautyshop.service.DonHang_Service;
import java.util.List;
import java.util.Map;
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
    String danhSach(@RequestParam(name = "status", required = false) List<OrderStatus> loc,
                    @RequestParam Map<String, String> thamSo, Model model) {
        var nguon = NguonDieuHuong.donAdmin(thamSo, loc, false);
        var trang = donHangService.danhSachAdmin(nguon.loc(), nguon.page());
        model.addAttribute("donHang", trang.getContent());
        model.addAttribute("orderOrigin", new NguonDonAdmin(nguon.loc(), trang.getNumber()));
        model.addAttribute("pager", PhanTrang.tu(trang, nguon::danhSach));
        model.addAttribute("boLoc", nguon.loc());
        model.addAttribute("trangThaiDon", OrderStatus.values());
        model.addAttribute("phuongThucThanhToan", BoLocDonHangAdmin.PhuongThuc.values());
        model.addAttribute("trangThaiThanhToan", BoLocDonHangAdmin.ThanhToan.values());
        model.addAttribute("sapXep", BoLocDonHangAdmin.SapXep.values());
        model.addAttribute("kichThuocTrang", BoLocDonHangAdmin.KICH_THUOC);
        model.addAttribute("tongQuan", donHangService.tongQuanAdmin());
        model.addAttribute("base", NguonDieuHuong.DON_ADMIN);
        return "order/list";
    }

    @GetMapping("/{id}")
    String chiTiet(@PathVariable long id, @RequestParam Map<String, String> thamSo,
                   @RequestParam(name = "listStatus", required = false) List<OrderStatus> loc, Model model) {
        var chiTiet = donHangService.chiTietAdmin(id);
        var nguon = NguonDieuHuong.donAdmin(thamSo, loc, true);
        model.addAttribute("chiTiet", chiTiet);
        model.addAttribute("nhanThanhToanAdmin", BoLocDonHangAdmin.ThanhToan.nhan(chiTiet.thanhToan()));
        model.addAttribute("base", NguonDieuHuong.DON_ADMIN);
        var ky = NguonDieuHuong.kyDashboard(thamSo);
        model.addAttribute("navDashboard", ky);
        model.addAttribute("listStatus", nguon.loc().trangThai());
        model.addAttribute("orderListFields", nguon.truongAn());
        model.addAttribute("backToOrders", nguon.quayLai(id));
        model.addAttribute("orderBack", ky.isEmpty() ? null : NguonDieuHuong.veDashboard(ky));
        return "order/detail";
    }

    @PostMapping("/{id}/status")
    String chuyen(@PathVariable long id, @RequestParam OrderStatus status, @RequestParam Map<String, String> thamSo,
                  @RequestParam(name = "listStatus", required = false) List<OrderStatus> loc, RedirectAttributes flash) {
        try {
            donHangService.chuyenAdmin(id, status);
            flash.addFlashAttribute("message", "Đã cập nhật trạng thái đơn.");
        } catch (IllegalStateException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        NguonDieuHuong.kyDashboard(thamSo).forEach(flash::addAttribute);
        NguonDieuHuong.donAdmin(thamSo, loc, true).giuQuaRedirect(flash);
        return "redirect:/admin/orders/" + id;
    }
}
