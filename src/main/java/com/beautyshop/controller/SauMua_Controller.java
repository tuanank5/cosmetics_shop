package com.beautyshop.controller;

import com.beautyshop.dto.DanhGia_Form;
import com.beautyshop.dto.DoiTra_Form;
import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.service.SauMua_Service;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class SauMua_Controller {
    private final SauMua_Service sauMua;

    SauMua_Controller(SauMua_Service sauMua) { this.sauMua = sauMua; }

    @InitBinder("doiTra")
    void bindDoiTra(WebDataBinder binder) {
        binder.setAllowedFields("lyDo", "soLuong[*]");
    }

    @GetMapping("/orders/items/{id}/review")
    String danhGia(@PathVariable long id, @AuthenticationPrincipal PhienNguoiDung phien, Model model) {
        var trang = sauMua.xemDanhGia(phien.getMaNguoiDung(), id);
        var form = new DanhGia_Form();
        if (trang.hienTai() != null) {
            form.setSoSao(trang.hienTai().soSao());
            form.setNoiDung(trang.hienTai().noiDung());
        }
        model.addAttribute("form", form);
        return hienDanhGia(id, trang, model);
    }

    @PostMapping({"/orders/items/{id}/review", "/orders/items/{id}/review/{action:edit}"})
    String luuDanhGia(@PathVariable long id, @PathVariable(required = false) String action,
                       @AuthenticationPrincipal PhienNguoiDung phien,
                       @Valid @ModelAttribute("form") DanhGia_Form form, BindingResult errors,
                       Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            return hienDanhGia(id, sauMua.xemDanhGia(phien.getMaNguoiDung(), id), model);
        }
        long maDon = sauMua.luuDanhGia(phien.getMaNguoiDung(), id, form, action != null);
        flash.addFlashAttribute("message", "Đã gửi đánh giá để duyệt.");
        return "redirect:/orders/" + maDon;
    }

    private String hienDanhGia(long id, SauMua_Service.TrangDanhGia trang, Model model) {
        model.addAttribute("trang", trang);
        model.addAttribute("action", "/orders/items/" + id + "/review" + (trang.hienTai() == null ? "" : "/edit"));
        return "order/review";
    }

    @GetMapping("/orders/{id}/returns/new")
    String taoDoiTra(@PathVariable long id, @AuthenticationPrincipal PhienNguoiDung phien, Model model) {
        model.addAttribute("doiTra", new DoiTra_Form());
        return hienDoiTra(id, phien, model);
    }

    @PostMapping("/orders/{id}/returns/new")
    String taoDoiTra(@PathVariable long id, @AuthenticationPrincipal PhienNguoiDung phien,
                      @Valid @ModelAttribute("doiTra") DoiTra_Form form, BindingResult errors,
                      Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                long ma = sauMua.yeuCau(phien.getMaNguoiDung(), id, form);
                flash.addFlashAttribute("message", "Đã gửi yêu cầu đổi trả.");
                return "redirect:/account/returns/" + ma;
            } catch (IllegalArgumentException exception) {
                errors.reject("return.quantities", exception.getMessage());
            }
        }
        return hienDoiTra(id, phien, model);
    }

    private String hienDoiTra(long id, PhienNguoiDung phien, Model model) {
        model.addAttribute("trang", sauMua.xemDon(phien.getMaNguoiDung(), id));
        return "return/form";
    }

    @GetMapping("/account/returns")
    String yeuCau(@AuthenticationPrincipal PhienNguoiDung phien, Model model) {
        model.addAttribute("yeuCau", sauMua.danhSachDoiTra(phien.getMaNguoiDung()));
        model.addAttribute("base", "/account/returns");
        return "return/list";
    }

    @GetMapping("/account/returns/{id}")
    String chiTiet(@PathVariable long id, @AuthenticationPrincipal PhienNguoiDung phien, Model model) {
        model.addAttribute("trang", sauMua.chiTiet(id, phien.getMaNguoiDung()));
        model.addAttribute("base", "/account/returns");
        return "return/detail";
    }

    @GetMapping("/account/wishlist")
    String yeuThich(@AuthenticationPrincipal PhienNguoiDung phien, Model model) {
        model.addAttribute("yeuThich", sauMua.danhSachYeuThich(phien.getMaNguoiDung()));
        return "auth/wishlist";
    }

    @PostMapping("/account/wishlist/{id}")
    String themYeuThich(@PathVariable long id, @AuthenticationPrincipal PhienNguoiDung phien) {
        sauMua.themYeuThich(phien.getMaNguoiDung(), id);
        return "redirect:/account/wishlist";
    }

    @PostMapping("/account/wishlist/{id}/delete")
    String xoaYeuThich(@PathVariable long id, @AuthenticationPrincipal PhienNguoiDung phien) {
        sauMua.xoaYeuThich(phien.getMaNguoiDung(), id);
        return "redirect:/account/wishlist";
    }
}
