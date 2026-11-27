package com.beautyshop.controller.admin;

import com.beautyshop.dto.PhanTrang;
import com.beautyshop.dto.BoLocDanhSachAdmin;
import com.beautyshop.controller.NguonDieuHuong;
import com.beautyshop.controller.NguonDieuHuong.NguonDanhSachAdmin;
import java.util.Map;
import com.beautyshop.dto.PhieuGiamGia_Form;
import com.beautyshop.service.PhieuGiamGia_Service;
import jakarta.validation.Valid;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/vouchers")
class AdminPhieuGiamGia_Controller {
    private final PhieuGiamGia_Service phieuGiamGiaService;

    AdminPhieuGiamGia_Controller(PhieuGiamGia_Service phieuGiamGiaService) {
        this.phieuGiamGiaService = phieuGiamGiaService;
    }

    @GetMapping
    String danhSach(@RequestParam(defaultValue = "") String q, @RequestParam(required = false) Boolean active,
                     @RequestParam(defaultValue = "0") int page, Model model) {
        var loc = new BoLocDanhSachAdmin(q, active, null, null);
        var trang = phieuGiamGiaService.danhSachAdmin(loc, PhanTrang.yeuCau(page, 15));
        model.addAttribute("danhSach", trang.getContent());
        model.addAttribute("boLoc", loc);
        model.addAttribute("listSource", new NguonDanhSachAdmin(loc.thamSo(), trang.getNumber()));
        model.addAttribute("pager", PhanTrang.tu(trang, so -> NguonDieuHuong.trangDanhSach("/admin/vouchers", loc.thamSo(), so)));
        return "admin/voucher/list";
    }

    @GetMapping({"/new", "/{id}/edit"})
    String hienForm(@PathVariable(required = false) Long id, @RequestParam Map<String, String> thamSo, Model model) {
        model.addAttribute("form", id == null ? new PhieuGiamGia_Form() : phieuGiamGiaService.docFormAdmin(id));
        return formView(id, thamSo, model);
    }

    @PostMapping({"/new", "/{id}/edit"})
    String luu(@PathVariable(required = false) Long id, @Valid @ModelAttribute("form") PhieuGiamGia_Form form,
               BindingResult errors, @RequestParam Map<String, String> thamSo, Model model, RedirectAttributes flash) {
        var ky = NguonDieuHuong.kyDashboard(thamSo);
        if (!errors.hasErrors()) {
            try {
                phieuGiamGiaService.luuAdmin(id, form, errors);
            } catch (DuplicateKeyException exception) {
                errors.rejectValue("maApDung", "voucher.duplicate", "Mã giảm giá đã tồn tại.");
            } catch (ConcurrencyFailureException exception) {
                errors.reject("voucher.retry", "Mã giảm giá đang được cập nhật. Vui lòng thử lại.");
            }
        }
        if (errors.hasErrors()) {
            return formView(id, thamSo, model);
        }
        flash.addFlashAttribute("message", "Đã lưu mã giảm giá.");
        return "redirect:" + (ky.isEmpty() ? NguonDieuHuong.nguonDanhSachAdmin(thamSo).quayLai("/admin/vouchers", id) : NguonDieuHuong.veDashboard(ky));
    }

    @PostMapping("/{id}/delete")
    String xoa(@PathVariable long id, @RequestParam Map<String, String> thamSo, RedirectAttributes flash) {
        try {
            phieuGiamGiaService.xoaAdmin(id);
            flash.addFlashAttribute("message", "Đã xóa mã giảm giá.");
        } catch (IllegalStateException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        } catch (ConcurrencyFailureException exception) {
            flash.addFlashAttribute("error", "Mã giảm giá đang được cập nhật. Vui lòng thử lại.");
        }
        return "redirect:" + NguonDieuHuong.nguonDanhSachAdmin(thamSo).quayLai("/admin/vouchers", id);
    }

    private String formView(Long id, Map<String, String> thamSo, Model model) {
        var ky = NguonDieuHuong.kyDashboard(thamSo);
        var nguon = NguonDieuHuong.nguonDanhSachAdmin(thamSo);
        model.addAttribute("listSourceFields", nguon.truongAn());
        model.addAttribute("backToVouchers", nguon.quayLai("/admin/vouchers", id));
        model.addAttribute("id", id);
        model.addAttribute("navDashboard", ky);
        model.addAttribute("voucherBack", ky.isEmpty() ? null : NguonDieuHuong.veDashboard(ky));
        return "admin/voucher/form";
    }
}
