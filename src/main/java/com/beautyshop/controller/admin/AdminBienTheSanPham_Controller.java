package com.beautyshop.controller.admin;

import com.beautyshop.dto.BienTheSanPham_Form;
import com.beautyshop.service.BienTheSanPham_Service;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/catalog/products/{productId}/variants")
class AdminBienTheSanPham_Controller {
    private final BienTheSanPham_Service bienTheService;

    AdminBienTheSanPham_Controller(BienTheSanPham_Service bienTheService) {
        this.bienTheService = bienTheService;
    }

    @GetMapping
    String danhSach(@PathVariable long productId, Model model) {
        model.addAttribute("sanPham", bienTheService.sanPham(productId));
        model.addAttribute("bienThe", bienTheService.danhSach(productId));
        return "admin/variant/list";
    }

    @GetMapping({"/new", "/{variantId}/edit"})
    String form(@PathVariable long productId, @PathVariable(required = false) Long variantId, Model model) {
        return hienForm(productId, variantId, model);
    }

    @PostMapping({"/new", "/{variantId}/edit"})
    String luu(@PathVariable long productId, @PathVariable(required = false) Long variantId,
               @Valid @ModelAttribute("form") BienTheSanPham_Form form, BindingResult errors,
               Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                bienTheService.luu(productId, variantId, form, errors);
            } catch (DataIntegrityViolationException exception) {
                errors.rejectValue("sku", "sku.conflict", "SKU đã được dùng hoặc sản phẩm đã thay đổi.");
            }
        }
        if (errors.hasErrors()) {
            return hienForm(productId, variantId, model);
        }
        flash.addFlashAttribute("message", "Đã lưu biến thể.");
        return "redirect:" + duongDan(productId);
    }

    @PostMapping("/{variantId}/delete")
    String ngungHoatDong(@PathVariable long productId, @PathVariable long variantId, RedirectAttributes flash) {
        bienTheService.ngungHoatDong(productId, variantId);
        flash.addFlashAttribute("message", "Đã ngừng bán biến thể.");
        return "redirect:" + duongDan(productId);
    }

    private String hienForm(long productId, Long variantId, Model model) {
        model.addAttribute("sanPham", bienTheService.sanPham(productId));
        var bienThe = variantId == null ? null : bienTheService.chiTiet(productId, variantId);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", bienThe == null ? new BienTheSanPham_Form() : BienTheSanPham_Form.from(bienThe));
        }
        if (bienThe != null) {
            model.addAttribute("anhHienTai", bienThe.getUrlHinhAnh());
        }
        model.addAttribute("action", duongDan(productId) + (variantId == null ? "/new" : "/" + variantId + "/edit"));
        return "admin/variant/form";
    }

    private String duongDan(long productId) {
        return "/admin/catalog/products/" + productId + "/variants";
    }
}
