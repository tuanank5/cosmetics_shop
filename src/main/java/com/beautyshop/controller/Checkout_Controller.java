package com.beautyshop.controller;

import com.beautyshop.dto.Checkout_Form;
import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.service.DiaChiNguoiDung_Service;
import com.beautyshop.service.DonHang_Service;
import com.beautyshop.service.GioHang_Service;
import com.beautyshop.service.Momo_Facade;
import com.beautyshop.service.PhieuGiamGia_Service;
import com.beautyshop.shipping.ShippingFee_Policy;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/checkout")
class Checkout_Controller {
    private final DonHang_Service donHangService;
    private final GioHang_Service gioHangService;
    private final DiaChiNguoiDung_Service diaChiService;
    private final PhieuGiamGia_Service voucherService;
    private final Momo_Facade momo;

    Checkout_Controller(DonHang_Service donHangService, GioHang_Service gioHangService,
                        DiaChiNguoiDung_Service diaChiService, PhieuGiamGia_Service voucherService, Momo_Facade momo) {
        this.donHangService = donHangService;
        this.gioHangService = gioHangService;
        this.diaChiService = diaChiService;
        this.voucherService = voucherService;
        this.momo = momo;
    }

    @PostMapping("/quote")
    String baoGia(@AuthenticationPrincipal PhienNguoiDung phien,
                  @Valid @ModelAttribute("form") Checkout_Form form, BindingResult errors, Model model) {
        hienCheckout(phien, model);
        if (!errors.hasErrors()) {
            BigDecimal tamTinh = (BigDecimal) model.getAttribute("tamTinh");
            try {
                themBaoGia(model, tamTinh, voucherService.xemTruoc(form.getVoucher(), tamTinh).soTien());
            } catch (IllegalArgumentException exception) {
                errors.rejectValue("voucher", "checkout.voucher", exception.getMessage());
            }
        }
        return "order/checkout";
    }

    @GetMapping
    String checkout(@AuthenticationPrincipal PhienNguoiDung phien, Model model) {
        model.addAttribute("form", new Checkout_Form());
        return hienCheckout(phien, model);
    }

    @PostMapping
    String datDon(@AuthenticationPrincipal PhienNguoiDung phien,
                  @RequestParam(defaultValue = "COD") String payment,
                  @Valid @ModelAttribute("form") Checkout_Form form, BindingResult errors,
                  Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                if ("MOMO".equals(payment)) {
                    long maDon = donHangService.datMomo(phien.getMaNguoiDung(), form.getAddressId(), form.getVoucher());
                    return batDauMomo(maDon, flash);
                }
                if (!"COD".equals(payment)) {
                    throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ.");
                }
                long maDon = donHangService.datCOD(phien.getMaNguoiDung(), form.getAddressId(), form.getVoucher());
                flash.addFlashAttribute("message", "Đã đặt đơn COD.");
                return "redirect:/orders/" + maDon;
            } catch (IllegalArgumentException | IllegalStateException exception) {
                errors.reject("checkout.invalid", exception.getMessage());
            } catch (ConcurrencyFailureException exception) {
                errors.reject("checkout.retry", "Giỏ hàng hoặc tồn kho vừa thay đổi. Vui lòng thử lại.");
            }
        }
        return hienCheckout(phien, model);
    }

    private String batDauMomo(long maDon, RedirectAttributes flash) {
        try {
            var url = momo.batDau(maDon);
            if (url.isPresent()) {
                return "redirect:" + url.get();
            }
        } catch (IllegalStateException | ConcurrencyFailureException exception) {
            // Đơn đã commit. Chờ đối soát khi chưa lấy được trang thanh toán.
        }
        flash.addFlashAttribute("error", "Đơn đang chờ thanh toán. Có thể mở lại thanh toán hoặc chờ đối soát.");
        return "redirect:/orders/" + maDon;
    }

    private String hienCheckout(PhienNguoiDung phien, Model model) {
        var diaChi = diaChiService.danhSach(phien.getMaNguoiDung());
        var muc = gioHangService.docGioCustomer(phien.getMaNguoiDung());
        var form = (Checkout_Form) model.getAttribute("form");
        if (!model.containsAttribute(BindingResult.MODEL_KEY_PREFIX + "form") && form.getAddressId() == null) {
            diaChi.stream().filter(dc -> dc.isLaMacDinh()).findFirst().ifPresent(dc -> form.setAddressId(dc.getMaDiaChi()));
        }
        model.addAttribute("diaChi", diaChi);
        model.addAttribute("muc", muc);
        model.addAttribute("momoEnabled", momo.enabled());
        model.addAttribute("momoMock", momo.mock());
        BigDecimal tamTinh = muc.stream().map(sku -> sku.giaBan().multiply(BigDecimal.valueOf(sku.soLuong())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        themBaoGia(model, tamTinh, BigDecimal.ZERO);
        return "order/checkout";
    }

    private void themBaoGia(Model model, BigDecimal tamTinh, BigDecimal giam) {
        BigDecimal sauGiam = tamTinh.subtract(giam);
        BigDecimal phi = ShippingFee_Policy.calculate(sauGiam);
        model.addAttribute("tamTinh", tamTinh);
        model.addAttribute("giam", giam);
        model.addAttribute("phi", phi);
        model.addAttribute("tong", sauGiam.add(phi));
    }
}
