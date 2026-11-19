package com.beautyshop.catalog;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/catalog")
class AdminCatalogController {
    private static final String ROOT = "/admin/catalog/";
    private final CatalogService catalog;

    AdminCatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    String index() {
        return "redirect:" + ROOT + CatalogSection.CATEGORIES.path;
    }

    @GetMapping("/{section}")
    String list(@PathVariable String section, Model model) {
        CatalogSection selected = CatalogSection.from(section);
        prepare(model, selected);
        model.addAttribute("items", catalog.list(selected));
        return "admin/catalog/list";
    }

    @GetMapping({"/{section}/new", "/{section}/{id}/edit"})
    String edit(@PathVariable String section, @PathVariable(required = false) Long id, Model model) {
        CatalogSection selected = CatalogSection.from(section);
        model.addAttribute("form", id == null ? new CatalogForm() : catalog.edit(selected, id));
        return formView(model, selected, id);
    }

    @PostMapping({"/{section}/new", "/{section}/{id}/edit"})
    String save(@PathVariable String section, @PathVariable(required = false) Long id,
                @Valid @ModelAttribute("form") CatalogForm form, BindingResult errors,
                Model model, RedirectAttributes flash) {
        CatalogSection selected = CatalogSection.from(section);
        if (!errors.hasErrors()) {
            try {
                catalog.save(selected, id, form, errors);
            } catch (DataIntegrityViolationException exception) {
                errors.reject("catalog.integrity", "Không thể lưu. Kiểm tra đường dẫn trùng hoặc danh mục và thương hiệu đã thay đổi.");
            }
        }
        if (errors.hasErrors()) {
            return formView(model, selected, id);
        }
        flash.addFlashAttribute("message", "Đã lưu.");
        return "redirect:" + ROOT + selected.path;
    }

    @PostMapping("/{section}/{id}/delete")
    String delete(@PathVariable String section, @PathVariable long id, RedirectAttributes flash) {
        CatalogSection selected = CatalogSection.from(section);
        try {
            catalog.remove(selected, id);
            flash.addFlashAttribute("message",
                selected == CatalogSection.PRODUCTS ? "Đã ngừng hiển thị sản phẩm." : "Đã xóa.");
        } catch (DataIntegrityViolationException exception) {
            flash.addFlashAttribute("error", "Mục này đang được sản phẩm sử dụng. Bạn có thể chuyển sang trạng thái ngừng hoạt động.");
        }
        return "redirect:" + ROOT + selected.path;
    }

    private String formView(Model model, CatalogSection selected, Long id) {
        prepare(model, selected);
        model.addAttribute("action", ROOT + selected.path + (id == null ? "/new" : "/" + id + "/edit"));
        model.addAttribute("heading", id == null ? "Thêm mới" : "Chỉnh sửa");
        if (selected == CatalogSection.PRODUCTS) {
            model.addAttribute("categories", catalog.list(CatalogSection.CATEGORIES));
            model.addAttribute("brands", catalog.list(CatalogSection.BRANDS));
        }
        return "admin/catalog/form";
    }

    private void prepare(Model model, CatalogSection selected) {
        model.addAttribute("section", selected.path);
        model.addAttribute("label", selected.label);
        model.addAttribute("product", selected == CatalogSection.PRODUCTS);
    }
}
