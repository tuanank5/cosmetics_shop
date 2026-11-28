package com.beautyshop.controller;

import com.beautyshop.cart.GioKhach;
import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.service.GioHang_Service;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.WebUtils;

@Controller
@RequestMapping("/cart")
class GioHang_Controller {
    private final GioHang_Service gioHangService;

    GioHang_Controller(GioHang_Service gioHangService) {
        this.gioHangService = gioHangService;
    }

    @GetMapping
    String gioHang(@AuthenticationPrincipal PhienNguoiDung nguoiDung, HttpSession session, Model model) {
        synchronized (WebUtils.getSessionMutex(session)) {
            var muc = nguoiDung == null ? gioHangService.docGioKhach(GioKhach.doc(session))
                : gioHangService.docGioCustomer(nguoiDung.getMaNguoiDung());
            model.addAttribute("mucGio", muc);
            if (GiaoDien_Advice.laCustomer(nguoiDung)) GiaoDien_Advice.hienSoLuongGio(model, muc);
        }
        return "cart/index";
    }

    @PostMapping("/items")
    String them(@RequestParam long variant, @AuthenticationPrincipal PhienNguoiDung nguoiDung,
                HttpSession session, RedirectAttributes flash) {
        try {
            if (nguoiDung == null) {
                synchronized (WebUtils.getSessionMutex(session)) {
                    themSauKhiKhoaPhien(session, variant);
                }
            } else {
                gioHangService.themGioCustomer(nguoiDung.getMaNguoiDung(), variant);
            }
            flash.addFlashAttribute("message", "Đã thêm biến thể sản phẩm vào giỏ hàng.");
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/cart";
    }

    private void themSauKhiKhoaPhien(HttpSession session, long maBienThe) {
        PhienNguoiDung phien = customerTrongPhien(session);
        if (phien == null) {
            gioHangService.themGioKhach(GioKhach.doc(session), maBienThe).luu(session);
        } else {
            gioHangService.themGioCustomer(phien.getMaNguoiDung(), maBienThe);
        }
    }

    @PostMapping("/items/{variant}/quantity")
    String datSoLuong(@PathVariable long variant, @RequestParam int quantity,
                     @AuthenticationPrincipal PhienNguoiDung nguoiDung, HttpSession session, RedirectAttributes flash) {
        try {
            synchronized (WebUtils.getSessionMutex(session)) {
                PhienNguoiDung phien = nguoiDung == null ? customerTrongPhien(session) : nguoiDung;
                if (phien == null) {
                    gioHangService.datSoLuongKhach(GioKhach.doc(session), variant, quantity).luu(session);
                } else {
                    gioHangService.datSoLuongCustomer(phien.getMaNguoiDung(), variant, quantity);
                }
            }
            flash.addFlashAttribute("message", quantity == 0 ? "Đã xóa biến thể sản phẩm khỏi giỏ hàng." : "Đã cập nhật số lượng.");
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/cart";
    }

    private PhienNguoiDung customerTrongPhien(HttpSession session) {
        // Một request Guest có thể đợi trong lúc handler đăng nhập hợp nhất giỏ.
        Object context = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        if (context instanceof SecurityContext securityContext
            && securityContext.getAuthentication() != null
            && securityContext.getAuthentication().getPrincipal() instanceof PhienNguoiDung phien) {
            if (phien.getAuthorities().stream().noneMatch(role -> "ROLE_CUSTOMER".equals(role.getAuthority()))) {
                throw new AccessDeniedException("Chỉ Customer mới có giỏ hàng.");
            }
            return phien;
        }
        return null;
    }
}
