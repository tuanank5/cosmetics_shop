package com.beautyshop.controller.admin;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/vouchers")
class AdminPhieuGiamGia_Controller {
    private final PhieuGiamGia_Service phieuGiamGiaService;

    AdminPhieuGiamGia_Controller(PhieuGiamGia_Service phieuGiamGiaService) {
        this.phieuGiamGiaService = phieuGiamGiaService;
    }

    @GetMapping
    String danhSach(Model model) {
        model.addAttribute("danhSach", phieuGiamGiaService.danhSachAdmin());
        return "admin/voucher/list";
    }

    @GetMapping({"/new", "/{id}/edit"})
    String hienForm(@PathVariable(required = false) Long id, Model model) {
        model.addAttribute("form", id == null ? new PhieuGiamGia_Form() : phieuGiamGiaService.docFormAdmin(id));
        return formView(id, model);
    }

    @PostMapping({"/new", "/{id}/edit"})
    String luu(@PathVariable(required = false) Long id, @Valid @ModelAttribute("form") PhieuGiamGia_Form form,
               BindingResult errors, Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                phieuGiamGiaService.luuAdmin(id, form, errors);
            } catch (DuplicateKeyException exception) {
                errors.rejectValue("maApDung", "voucher.duplicate", "Mã voucher đã tồn tại.");
            } catch (ConcurrencyFailureException exception) {
                errors.reject("voucher.retry", "Voucher đang được cập nhật. Vui lòng thử lại.");
            }
        }
        if (errors.hasErrors()) {
            return formView(id, model);
        }
        flash.addFlashAttribute("message", "Đã lưu voucher.");
        return "redirect:/admin/vouchers";
    }

    @PostMapping("/{id}/delete")
    String xoa(@PathVariable long id, RedirectAttributes flash) {
        try {
            phieuGiamGiaService.xoaAdmin(id);
            flash.addFlashAttribute("message", "Đã xóa voucher.");
        } catch (IllegalStateException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        } catch (ConcurrencyFailureException exception) {
            flash.addFlashAttribute("error", "Voucher đang được cập nhật. Vui lòng thử lại.");
        }
        return "redirect:/admin/vouchers";
    }

    private String formView(Long id, Model model) {
        model.addAttribute("id", id);
        return "admin/voucher/form";
    }
}
