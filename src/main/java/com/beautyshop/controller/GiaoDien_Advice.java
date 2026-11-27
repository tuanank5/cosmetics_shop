package com.beautyshop.controller;

import com.beautyshop.cart.GioKhach;
import com.beautyshop.config.LienHeCuaHang;
import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.service.GioHang_Service;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Read-only layout data; never creates or updates a cart. */
@ControllerAdvice
public class GiaoDien_Advice {
    private final ObjectProvider<GioHang_Service> gioHangService;
    private final LienHeCuaHang lienHe;

    public GiaoDien_Advice(ObjectProvider<GioHang_Service> gioHangService,
                         @Value("${BEAUTYSHOP_CONTACT_PHONE:}") String dienThoai,
                         @Value("${BEAUTYSHOP_CONTACT_EMAIL:}") String email,
                         @Value("${BEAUTYSHOP_ZALO_URL:}") String zalo,
                         @Value("${BEAUTYSHOP_FACEBOOK_URL:}") String facebook,
                         @Value("${BEAUTYSHOP_INSTAGRAM_URL:}") String instagram) {
        this.gioHangService = gioHangService;
        this.lienHe = new LienHeCuaHang(dienThoai, email, zalo, facebook, instagram);
    }

    @ModelAttribute
    public void khungTrang(HttpServletRequest request, @AuthenticationPrincipal PhienNguoiDung phien, Model model) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        model.addAttribute("uiPath", path);
        model.addAttribute("lienHeCuaHang", lienHe);
        if (!"GET".equals(request.getMethod()) && !"POST".equals(request.getMethod())) return;
        if (path.equals("/error") || path.startsWith("/payment/") || path.startsWith("/admin/")) return;
        if (phien == null) {
            model.addAttribute("uiCartCount", GioKhach.doc(request.getSession(false)).soLuong().values()
                .stream().mapToLong(Integer::longValue).sum());
        } else if (phien.getAuthorities().stream().anyMatch(quyen -> quyen.getAuthority().equals("ROLE_CUSTOMER"))) {
            var service = gioHangService.getIfAvailable();
            if (service != null) {
                try {
                    model.addAttribute("uiCartCount", service.docGioCustomer(phien.getMaNguoiDung())
                        .stream().mapToLong(muc -> muc.soLuong()).sum());
                } catch (DataAccessException exception) {
                    // The optional badge must not prevent the page from rendering.
                }
            }
        }
    }
}
