package com.beautyshop.controller;

import com.beautyshop.dto.PhanTrang;
import com.beautyshop.dto.BoLocDonHangAdmin.ThanhToan;
import com.beautyshop.order.OrderStatus;
import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.service.DonHang_Service;
import com.beautyshop.service.SauMua_Service;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/orders")
class DonHang_Controller {
    private static final List<OrderStatus> TIEN_TRINH = List.of(OrderStatus.PENDING_PAYMENT,
        OrderStatus.PENDING_CONFIRMATION, OrderStatus.CONFIRMED, OrderStatus.PACKING,
        OrderStatus.SHIPPING, OrderStatus.DELIVERED);

    private final DonHang_Service donHangService;
    private final SauMua_Service sauMua;

    DonHang_Controller(DonHang_Service donHangService, SauMua_Service sauMua) {
        this.donHangService = donHangService;
        this.sauMua = sauMua;
    }

    @GetMapping
    String danhSach(@AuthenticationPrincipal PhienNguoiDung phien,
                     @RequestParam(defaultValue = "0") int page, Model model) {
        var trang = donHangService.danhSachCustomer(phien.getMaNguoiDung(), PhanTrang.yeuCau(page, 10));
        model.addAttribute("donHang", trang.getContent());
        model.addAttribute("pager", PhanTrang.tu(trang, so -> NguonDieuHuong.trangDanhSach("/orders", Map.of(), so)));
        model.addAttribute("base", "/orders");
        return "order/list";
    }

    @GetMapping("/{id}")
    String chiTiet(@PathVariable long id, @AuthenticationPrincipal PhienNguoiDung phien,
                   @RequestParam Map<String, String> thamSo, Model model) {
        var chiTiet = donHangService.chiTietCustomer(id);
        var don = chiTiet.don();
        List<OrderStatus> tienTrinh = "MOMO".equals(don.phuongThuc())
            ? TIEN_TRINH : TIEN_TRINH.subList(1, TIEN_TRINH.size());
        model.addAttribute("chiTiet", chiTiet);
        model.addAttribute("backToCustomerOrders", NguonDieuHuong.veDanhSachKhach("/orders", thamSo, "customer-order", id));
        model.addAttribute("soSanPham", chiTiet.muc().stream().mapToLong(muc -> muc.soLuong()).sum());
        model.addAttribute("tienTrinh", tienTrinh);
        model.addAttribute("buocHienTai", tienTrinh.indexOf(don.trangThai()));
        model.addAttribute("nhanThanhToan", ThanhToan.nhan(chiTiet.thanhToan()));
        if (don.trangThai() == OrderStatus.DELIVERED) {
            var doiTra = sauMua.xemDon(phien.getMaNguoiDung(), id);
            model.addAttribute("coTheYeuCauDoiTra", doiTra.muc().stream().anyMatch(muc -> muc.conLai() > 0));
            model.addAttribute("yeuCauDoiTra", sauMua.danhSachDoiTraCuaDon(phien.getMaNguoiDung(), id));
        }
        return "order/customer-detail";
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
