package com.beautyshop.controller;

import com.beautyshop.dto.Checkout_Form;
import com.beautyshop.dto.DiaChiHienThi;
import com.beautyshop.repository.GioHang_Repository.MucGio;
import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.service.DiaChiNguoiDung_Service;
import com.beautyshop.service.DonHang_Service;
import com.beautyshop.service.GioHang_Service;
import com.beautyshop.service.Momo_Facade;
import com.beautyshop.service.PhieuGiamGia_Service;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
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
        tinhBaoGia(phien.getMaNguoiDung(), form, errors, model, chuanBiCheckout(phien, model));
        return "order/checkout";
    }

    private void tinhBaoGia(long khach, Checkout_Form form, BindingResult errors, Model model, List<MucGio> mucGio) {
        if (mucGio.isEmpty()) {
            errors.reject("checkout.empty", "Giỏ hàng đang trống. Vui lòng quay lại giỏ hàng.");
        } else if (mucGio.stream().anyMatch(muc -> muc.soLuong() > muc.khaDung())) {
            errors.reject("checkout.stock", "Số lượng trong giỏ vượt số hàng còn bán được. Hãy điều chỉnh giỏ hàng.");
        }
        if (!errors.hasFieldErrors("addressId") && !Boolean.TRUE.equals(model.getAttribute("diaChiHopLe"))) {
            errors.rejectValue("addressId", "checkout.address", "Chọn địa chỉ giao hàng của bạn.");
        }
        if (!errors.hasErrors()) {
            try {
                var gia = voucherService.xemGio(form.getVoucher(), form.getShip(), khach, mucGio);
                model.addAttribute("dieuKienMa", java.util.stream.Stream.of(gia.hang().dieuKien(), gia.ship().dieuKien())
                    .filter(java.util.Objects::nonNull).toList());
                model.addAttribute("phi", gia.phiChuan());
                model.addAttribute("giam", gia.hang().soTien());
                model.addAttribute("giamShip", gia.ship().soTien());
                model.addAttribute("tong", gia.tong());
                model.addAttribute("baoGiaHopLe", true);
                model.addAttribute("voucherDaTinh", form.getVoucher() == null ? "" : form.getVoucher().strip());
            } catch (PhieuGiamGia_Service.MaVanChuyenKhongHopLe exception) {
                errors.rejectValue("ship", "checkout.ship", exception.getMessage());
            } catch (IllegalArgumentException exception) {
                errors.rejectValue("voucher", "checkout.voucher", exception.getMessage());
            } catch (IllegalStateException | ConcurrencyFailureException exception) {
                errors.reject("checkout.quote", "Chưa thể tính tổng tiền. Vui lòng thử lại báo giá.");
                model.addAttribute("baoGiaLoiDichVu", true);
            }
        }
    }

    @GetMapping
    String checkout(@AuthenticationPrincipal PhienNguoiDung phien, Model model) {
        var form = new Checkout_Form();
        model.addAttribute("form", form);
        var muc = chuanBiCheckout(phien, model);
        if (form.getAddressId() != null && !muc.isEmpty()) {
            var errors = new BeanPropertyBindingResult(form, "form");
            tinhBaoGia(phien.getMaNguoiDung(), form, errors, model, muc);
            model.addAllAttributes(errors.getModel());
        }
        return "order/checkout";
    }

    @PostMapping
    String datDon(@AuthenticationPrincipal PhienNguoiDung phien,
                  @RequestParam(defaultValue = "COD") String payment,
                  @Valid @ModelAttribute("form") Checkout_Form form, BindingResult errors,
                  Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                if ("MOMO".equals(payment)) {
                    long maDon = donHangService.datMomo(phien.getMaNguoiDung(), form.getAddressId(), form.getVoucher(), form.getShip());
                    return batDauMomo(maDon, flash);
                }
                if (!"COD".equals(payment)) {
                    throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ.");
                }
                long maDon = donHangService.datCOD(phien.getMaNguoiDung(), form.getAddressId(), form.getVoucher(), form.getShip());
                flash.addFlashAttribute("message", "Đã đặt đơn COD.");
                return "redirect:/orders/" + maDon;
            } catch (IllegalArgumentException | IllegalStateException exception) {
                errors.reject("checkout.invalid", exception.getMessage());
                model.addAttribute("baoGiaLoiDichVu", true);
            } catch (ConcurrencyFailureException exception) {
                errors.reject("checkout.retry", "Giỏ hàng hoặc tồn kho vừa thay đổi. Vui lòng thử lại.");
                model.addAttribute("baoGiaLoiDichVu", true);
            }
        }
        chuanBiCheckout(phien, model);
        model.addAttribute("payment", payment);
        return "order/checkout";
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
        flash.addFlashAttribute("error", "Đơn đang chờ thanh toán. Bạn có thể mở lại trang thanh toán hoặc chờ xác nhận kết quả.");
        return "redirect:/orders/" + maDon;
    }

    private List<MucGio> chuanBiCheckout(PhienNguoiDung phien, Model model) {
        var diaChi = diaChiService.danhSach(phien.getMaNguoiDung());
        var muc = gioHangService.docGioCustomer(phien.getMaNguoiDung());
        var form = (Checkout_Form) model.getAttribute("form");
        if (!model.containsAttribute(BindingResult.MODEL_KEY_PREFIX + "form") && form.getAddressId() == null) {
            diaChi.stream().filter(dc -> dc.laMacDinh() || diaChi.size() == 1).findFirst()
                .ifPresent(dc -> form.setAddressId(dc.maDiaChi()));
        }
        model.addAttribute("diaChi", diaChi);
        model.addAttribute("muc", muc);
        GiaoDien_Advice.hienSoLuongGio(model, muc);
        model.addAttribute("momoEnabled", momo.enabled());
        model.addAttribute("momoMock", momo.mock());
        BigDecimal tamTinh = muc.stream().map(MucGio::thanhTien)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("tamTinh", tamTinh);
        model.addAttribute("baoGiaHopLe", false);
        model.addAttribute("diaChiHopLe", diaChi.stream().anyMatch(dc -> dc.maDiaChi().equals(form.getAddressId())));
        return muc;
    }

}
