package com.beautyshop.controller.admin;

import com.beautyshop.dto.PhanTrang;
import com.beautyshop.controller.NguonDieuHuong;
import com.beautyshop.controller.NguonDieuHuong.NguonBienTheAdmin;
import java.util.Map;
import com.beautyshop.dto.BienTheSanPham_Form;
import com.beautyshop.service.BienTheSanPham_Service;
import com.beautyshop.service.NgungHoatDongCatalog_Service.ConDonDangXuLy;
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
    String danhSach(@PathVariable long productId, @RequestParam(defaultValue = "0") int page,
                    @RequestParam Map<String, String> thamSo, Model model) {
        model.addAttribute("sanPham", bienTheService.sanPham(productId));
        var trang = bienTheService.danhSach(productId, PhanTrang.yeuCau(page, 15));
        String path = "/admin/catalog/products/" + productId + "/variants";
        var nguon = new NguonBienTheAdmin(NguonDieuHuong.nguonDanhSachAdmin(thamSo), trang.getNumber());
        var fields = nguon.truongAn();
        model.addAttribute("backToProducts", nguon.sanPham().quayLai("/admin/catalog/products", productId));
        model.addAttribute("variantQuery", NguonDieuHuong.coThamSo("", fields));
        model.addAttribute("listSourceFields", fields);
        model.addAttribute("bienThe", trang.getContent());
        model.addAttribute("pager", PhanTrang.tu(trang, so -> NguonDieuHuong.trangDanhSach(path, nguon.sanPham().truongAn(), so)));
        model.addAttribute("donDangXuLy", bienTheService.demDonDangXuLy(productId));
        return "admin/variant/list";
    }

    @GetMapping({"/new", "/{variantId}/edit"})
    String form(@PathVariable long productId, @PathVariable(required = false) Long variantId,
                @RequestParam Map<String, String> thamSo, Model model) {
        return hienForm(productId, variantId, thamSo, model);
    }

    @PostMapping({"/new", "/{variantId}/edit"})
    String luu(@PathVariable long productId, @PathVariable(required = false) Long variantId,
               @Valid @ModelAttribute("form") BienTheSanPham_Form form, BindingResult errors,
               @RequestParam Map<String, String> thamSo, Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                bienTheService.luu(productId, variantId, form, errors);
            } catch (ConDonDangXuLy exception) {
                errors.rejectValue("dangHoatDong", "sku.active.orders", exception.getMessage());
            } catch (DataIntegrityViolationException exception) {
                errors.rejectValue("sku", "sku.conflict", "Mã biến thể sản phẩm đã được dùng hoặc thông tin sản phẩm vừa thay đổi.");
            }
        }
        if (errors.hasErrors()) {
            return hienForm(productId, variantId, thamSo, model);
        }
        flash.addFlashAttribute("message", "Đã lưu biến thể sản phẩm.");
        return "redirect:" + NguonDieuHuong.nguonBienTheAdmin(thamSo).veDanhSach(productId, variantId);
    }

    @PostMapping("/{variantId}/delete")
    String ngungHoatDong(@PathVariable long productId, @PathVariable long variantId, RedirectAttributes flash) {
        return trangThai(productId, variantId, false, Map.of(), flash);
    }

    @PostMapping("/{variantId}/status")
    String trangThai(@PathVariable long productId, @PathVariable long variantId, @RequestParam boolean active,
                     @RequestParam Map<String, String> thamSo, RedirectAttributes flash) {
        try {
            bienTheService.datTrangThai(productId, variantId, active);
            flash.addFlashAttribute("message", active ? "Đã kích hoạt lại biến thể." : "Đã ngừng bán biến thể sản phẩm.");
        } catch (ConDonDangXuLy exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:" + NguonDieuHuong.nguonBienTheAdmin(thamSo).veDanhSach(productId, variantId);
    }

    private String hienForm(long productId, Long variantId, Map<String, String> thamSo, Model model) {
        var nguon = NguonDieuHuong.nguonBienTheAdmin(thamSo);
        model.addAttribute("backToProducts", nguon.sanPham().quayLai("/admin/catalog/products", productId));
        model.addAttribute("backToVariants", nguon.veDanhSach(productId, variantId));
        model.addAttribute("sanPham", bienTheService.sanPham(productId));
        var bienThe = variantId == null ? null : bienTheService.chiTiet(productId, variantId);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", bienThe == null ? new BienTheSanPham_Form() : BienTheSanPham_Form.from(bienThe));
        }
        if (bienThe != null) {
            model.addAttribute("anhHienTai", bienThe.getUrlHinhAnh());
            model.addAttribute("soDonDangXuLy", bienThe.isDangHoatDong() ? bienTheService.demDonCuaBienThe(variantId) : 0L);
        }
        String action = duongDan(productId) + (variantId == null ? "/new" : "/" + variantId + "/edit");
        model.addAttribute("action", NguonDieuHuong.coThamSo(action, nguon.truongAn()));
        return "admin/variant/form";
    }

    private String duongDan(long productId) {
        return "/admin/catalog/products/" + productId + "/variants";
    }
}
