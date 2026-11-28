package com.beautyshop.controller;

import com.beautyshop.dto.BoLocDonHangAdmin.ThanhToan;
import com.beautyshop.dto.BoLocDonHangAdmin;
import com.beautyshop.dto.PhanTrang;
import com.beautyshop.order.OrderStatus;
import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.service.DonHang_Service;
import com.beautyshop.service.GioHang_Service;
import com.beautyshop.service.SauMua_Service;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    private final GioHang_Service gioHang;

    private final PhienMuaLai phienMuaLai;

    DonHang_Controller(DonHang_Service donHangService, SauMua_Service sauMua, GioHang_Service gioHang, PhienMuaLai phienMuaLai) {
        this.phienMuaLai = phienMuaLai;
        this.gioHang = gioHang;
        this.donHangService = donHangService;
        this.sauMua = sauMua;
    }

    @GetMapping
    String danhSach(@AuthenticationPrincipal PhienNguoiDung phien,
                     @RequestParam Map<String, String> thamSo,
                     @RequestParam(name = "status", required = false) List<OrderStatus> trangThai, Model model) {
        var nguon = NguonDieuHuong.donKhach(thamSo, trangThai, false);
        var trang = donHangService.danhSachCustomer(phien.getMaNguoiDung(), nguon.loc(), PhanTrang.yeuCau(nguon.page(), 10));
        model.addAttribute("donHang", trang.getContent());
        model.addAttribute("pager", PhanTrang.tu(trang, nguon::danhSachKhach));
        var loc = nguon.loc();
        model.addAttribute("dangLoc", !loc.maDon().isEmpty() || !loc.trangThai().isEmpty()
            || loc.tuNgay() != null || loc.denNgay() != null);
        model.addAttribute("customerOrigin", nguon);
        model.addAttribute("boLoc", nguon.loc());
        model.addAttribute("trangThaiDon", OrderStatus.values());
        model.addAttribute("sapXep", BoLocDonHangAdmin.SapXep.values());
        model.addAttribute("base", "/orders");
        return "order/list";
    }

    @GetMapping("/{id}")
    String chiTiet(@PathVariable long id, @AuthenticationPrincipal PhienNguoiDung phien,
                   @RequestParam Map<String, String> thamSo,
                   @RequestParam(name = "listStatus", required = false) List<OrderStatus> trangThai, Model model) {
        var chiTiet = donHangService.chiTietCustomer(id);
        var don = chiTiet.don();
        List<OrderStatus> tienTrinh = "MOMO".equals(don.phuongThuc())
            ? TIEN_TRINH : TIEN_TRINH.subList(1, TIEN_TRINH.size());
        model.addAttribute("chiTiet", chiTiet);
        var nguon = NguonDieuHuong.donKhach(thamSo, trangThai, true);
        model.addAttribute("backToCustomerOrders", nguon.page() < 0 ? "/orders" :
            nguon.danhSachKhach(nguon.page()) + "#customer-order-" + id);
        var requestId = UUID.randomUUID();
        model.addAttribute("maMuaLai", requestId);
        model.addAttribute("chuKyMuaLai", phienMuaLai.ky(phien.getMaNguoiDung(), id, requestId));
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

    @PostMapping("/{id}/reorder")
    String muaLai(@PathVariable long id, @AuthenticationPrincipal PhienNguoiDung phien,
                  @RequestParam UUID requestId, @RequestParam String proof, RedirectAttributes flash) {
        phienMuaLai.kiemTra(phien.getMaNguoiDung(), id, requestId, proof);
        var ketQua = gioHang.muaLai(id, requestId);
        if (ketQua.isEmpty()) flash.addFlashAttribute("message", "Yêu cầu đã được xử lý hoặc đơn không có sản phẩm.");
        else flash.addFlashAttribute("ketQuaMuaLai", ketQua);
        return "redirect:/cart";
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
