package com.beautyshop.controller.admin;

import com.beautyshop.domain.catalog.NhomDanhMucSanPhamThuongHieu;
import com.beautyshop.dto.DanhMucSanPhamThuongHieu_Form;
import com.beautyshop.service.DanhMucSanPhamThuongHieu_Service;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/catalog")
class AdminDanhMucSanPhamThuongHieu_Controller {
    private static final String ROOT = "/admin/catalog/";
    private final DanhMucSanPhamThuongHieu_Service danhMucSanPhamThuongHieuService;

    AdminDanhMucSanPhamThuongHieu_Controller(DanhMucSanPhamThuongHieu_Service danhMucSanPhamThuongHieuService) {
        this.danhMucSanPhamThuongHieuService = danhMucSanPhamThuongHieuService;
    }

    @GetMapping
    String index() {
        return "redirect:" + ROOT + NhomDanhMucSanPhamThuongHieu.DANH_MUC.path;
    }

    @GetMapping("/{section}")
    String list(@PathVariable String section, Model model) {
        NhomDanhMucSanPhamThuongHieu nhomDaChon = NhomDanhMucSanPhamThuongHieu.from(section);
        prepare(model, nhomDaChon);
        model.addAttribute("items", danhMucSanPhamThuongHieuService.list(nhomDaChon));
        return "admin/catalog/list";
    }

    @GetMapping({"/{section}/new", "/{section}/{id}/edit"})
    String edit(@PathVariable String section, @PathVariable(required = false) Long id, Model model) {
        NhomDanhMucSanPhamThuongHieu nhomDaChon = NhomDanhMucSanPhamThuongHieu.from(section);
        model.addAttribute("form", id == null ? new DanhMucSanPhamThuongHieu_Form() : danhMucSanPhamThuongHieuService.edit(nhomDaChon, id));
        return formView(model, nhomDaChon, id);
    }

    @PostMapping({"/{section}/new", "/{section}/{id}/edit"})
    String save(@PathVariable String section, @PathVariable(required = false) Long id,
                @Valid @ModelAttribute("form") DanhMucSanPhamThuongHieu_Form form, BindingResult errors,
                Model model, RedirectAttributes flash) {
        NhomDanhMucSanPhamThuongHieu nhomDaChon = NhomDanhMucSanPhamThuongHieu.from(section);
        if (!errors.hasErrors()) {
            try {
                danhMucSanPhamThuongHieuService.save(nhomDaChon, id, form, errors);
            } catch (DataIntegrityViolationException exception) {
                errors.reject("catalog.integrity", "Không thể lưu. Hãy kiểm tra tên dùng trong đường dẫn có bị trùng, hoặc danh mục và thương hiệu có vừa thay đổi hay không.");
            }
        }
        if (errors.hasErrors()) {
            return formView(model, nhomDaChon, id);
        }
        flash.addFlashAttribute("message", "Đã lưu.");
        return "redirect:" + ROOT + nhomDaChon.path;
    }

    @PostMapping("/{section}/{id}/status")
    String trangThai(@PathVariable String section, @PathVariable long id, @RequestParam boolean active,
                     RedirectAttributes flash) {
        NhomDanhMucSanPhamThuongHieu nhomDaChon = NhomDanhMucSanPhamThuongHieu.from(section);
        danhMucSanPhamThuongHieuService.datTrangThai(nhomDaChon, id, active);
        flash.addFlashAttribute("message", active ? "Đã kích hoạt lại." : "Đã ngừng hoạt động.");
        return "redirect:" + ROOT + nhomDaChon.path;
    }

    @PostMapping("/products/{id}/delete")
    String ngungHienThiSanPham(@PathVariable long id, RedirectAttributes flash) {
        danhMucSanPhamThuongHieuService.datTrangThai(NhomDanhMucSanPhamThuongHieu.SAN_PHAM, id, false);
        flash.addFlashAttribute("message", "Đã ngừng hiển thị sản phẩm.");
        return "redirect:" + ROOT + NhomDanhMucSanPhamThuongHieu.SAN_PHAM.path;
    }

    private String formView(Model model, NhomDanhMucSanPhamThuongHieu nhomDaChon, Long id) {
        prepare(model, nhomDaChon);
        model.addAttribute("action", ROOT + nhomDaChon.path + (id == null ? "/new" : "/" + id + "/edit"));
        model.addAttribute("heading", id == null ? "Thêm mới" : "Chỉnh sửa");
        if (nhomDaChon == NhomDanhMucSanPhamThuongHieu.SAN_PHAM) {
            model.addAttribute("categories", danhMucSanPhamThuongHieuService.list(NhomDanhMucSanPhamThuongHieu.DANH_MUC));
            model.addAttribute("brands", danhMucSanPhamThuongHieuService.list(NhomDanhMucSanPhamThuongHieu.THUONG_HIEU));
        }
        return "admin/catalog/form";
    }

    private void prepare(Model model, NhomDanhMucSanPhamThuongHieu nhomDaChon) {
        model.addAttribute("section", nhomDaChon.path);
        model.addAttribute("label", nhomDaChon.label);
        model.addAttribute("product", nhomDaChon == NhomDanhMucSanPhamThuongHieu.SAN_PHAM);
    }
}
